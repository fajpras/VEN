<?php

namespace App\Mail;

use App\Models\Undangan;
use Illuminate\Mail\Mailable;
use Illuminate\Mail\Mailables\Content;
use Illuminate\Mail\Mailables\Envelope;

final class UndanganDitanggapiMail extends Mailable
{
    public function __construct(
        public readonly Undangan $undangan,
        public readonly ?string $deepLink = null,
    ) {}

    public function envelope(): Envelope
    {
        $judul = $this->undangan->ruangan?->pameran?->judul ?? 'Pameran Virtual';
        $statusLabel = $this->undangan->status === 'disetujui' ? 'Disetujui' : 'Ditolak';

        return new Envelope(
            subject: "Undangan Diskusi Privat {$statusLabel} — {$judul}",
        );
    }

    public function content(): Content
    {
        return new Content(view: 'mail.undangan-ditanggapi');
    }
}
