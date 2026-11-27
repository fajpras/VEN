<?php

namespace App\Http\Controllers\Pengguna;

use App\Http\Controllers\Controller;
use App\Mail\UndanganDitanggapiMail;
use App\Models\Undangan;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\DB;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Mail;
use Illuminate\Support\Str;

/**
 * Menangani tombol Signed URL Setujui / Tolak langsung dari email undangan.
 */
class UndanganTautanWebController extends Controller
{
    public function tanggapi(Request $request, int $undangan, string $aksi)
    {
        if (! in_array($aksi, ['setujui', 'tolak'], true)) {
            abort(404);
        }

        $prevConnection = DB::getDefaultConnection();
        if ($prevConnection === 'mysql' && ! app()->environment('testing')) {
            DB::setDefaultConnection('mysql_user');
        }

        try {
            $model = Undangan::with(['ruangan.pameran', 'pengguna'])->findOrFail($undangan);

            if ($model->status !== 'diproses') {
                return view('undangan.tanggapan-hasil', [
                    'judul'    => 'Undangan Sudah Ditanggapi',
                    'pesan'    => 'Status undangan ini sebelumnya sudah ditetapkan menjadi "'.$model->status.'".',
                    'deepLink' => $model->status === 'disetujui' && $model->tautan ? 'ven://diskusi/'.$model->tautan : null,
                ]);
            }

            if ($aksi === 'setujui') {
                $model->forceFill([
                    'status'          => 'disetujui',
                    'tautan'          => Str::random(32),
                    'kadaluarsa_pada' => $model->waktu?->copy()->endOfDay() ?? now()->endOfDay(),
                ])->save();

                $deepLink = 'ven://diskusi/'.$model->tautan;
                $this->kirimEmailPengaju($model, $deepLink);

                return view('undangan.tanggapan-hasil', [
                    'judul'    => 'Undangan Disetujui',
                    'pesan'    => 'Tautan ruang diskusi privat (dengan akses Voice Chat) telah dikirim ke email pengaju dan berlaku hingga pukul 00:00 di akhir hari jadwal.',
                    'deepLink' => $deepLink,
                ]);
            }

            $model->forceFill(['status' => 'ditolak'])->save();
            $this->kirimEmailPengaju($model, null);

            return view('undangan.tanggapan-hasil', [
                'judul'    => 'Undangan Ditolak',
                'pesan'    => 'Permintaan diskusi privat telah ditolak dan pengaju sudah diberi tahu melalui email.',
                'deepLink' => null,
            ]);
        } finally {
            DB::setDefaultConnection($prevConnection);
        }
    }

    private function kirimEmailPengaju(Undangan $undangan, ?string $deepLink): void
    {
        $email = $undangan->pengguna?->email;
        if (! $email) {
            return;
        }

        try {
            Mail::to($email)->send(new UndanganDitanggapiMail($undangan, $deepLink));
        } catch (\Throwable $e) {
            Log::warning('Gagal mengirim email UndanganDitanggapiMail (web): '.$e->getMessage());
        }
    }
}
