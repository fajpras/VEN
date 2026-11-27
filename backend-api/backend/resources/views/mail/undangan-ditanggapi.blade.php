<!DOCTYPE html>
<html lang="id">
<head><meta charset="UTF-8"><title>Status Undangan Diskusi Privat</title></head>
<body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background:#f5f5f8; color:#18181b; margin:0; padding:24px;">
  <div style="max-width:520px; margin:0 auto; background:#ffffff; border:1px solid #e6e6ec; border-radius:14px; padding:28px;">
    <p style="margin:0 0 8px; font-size:13px; font-weight:700; color:#ba18f5; letter-spacing:1px;">VEN · DISKUSI PRIVAT</p>
    @if($undangan->status === 'disetujui')
      <h2 style="margin:0 0 12px; font-size:20px; font-weight:700;">Undangan Anda Disetujui</h2>
      <p style="margin:0 0 16px; font-size:14px; color:#52525b; line-height:1.6;">
        Sesi diskusi privat untuk pameran <strong>{{ $undangan->ruangan?->pameran?->judul ?? 'Pameran 3D' }}</strong> telah disetujui.
        Ruang privat ini terpisah dari ruang utama dan dilengkapi akses <strong>Voice Chat</strong> eksklusif.
      </p>
      <div style="background:#f5f5f8; border-radius:10px; padding:14px 16px; margin-bottom:20px; font-size:14px; line-height:1.7;">
        <div><strong>Jadwal:</strong> {{ $undangan->waktu?->format('d M Y, H:i') }} ({{ $undangan->durasi_menit ?? 30 }} menit)</div>
        <div><strong>Berlaku hingga:</strong> {{ $undangan->batasKadaluarsa()?->format('d M Y, 23:59') }} (otomatis berakhir 00:00)</div>
        @if($deepLink)
          <div style="margin-top:8px; word-break:break-all;"><strong>Tautan Khusus:</strong> {{ $deepLink }}</div>
        @endif
      </div>
      @if($deepLink)
        <a href="{{ $deepLink }}" style="display:inline-block; background:#ba18f5; color:#ffffff; text-decoration:none; font-weight:600; font-size:14px; padding:11px 20px; border-radius:10px;">Masuk Ruang Privat di VEN</a>
      @endif
    @else
      <h2 style="margin:0 0 12px; font-size:20px; font-weight:700;">Undangan Anda Ditolak</h2>
      <p style="margin:0 0 16px; font-size:14px; color:#52525b; line-height:1.6;">
        Mohon maaf, permintaan diskusi privat untuk pameran <strong>{{ $undangan->ruangan?->pameran?->judul ?? 'Pameran 3D' }}</strong> belum dapat disetujui.
      </p>
      @if($undangan->catatan)
        <div style="background:#f5f5f8; border-radius:10px; padding:14px 16px; font-size:14px;">
          <strong>Catatan:</strong> {{ $undangan->catatan }}
        </div>
      @endif
    @endif
  </div>
</body>
</html>
