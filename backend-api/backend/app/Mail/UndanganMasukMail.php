<?php

namespace App\Mail;

use App\Models\Undangan;
use Illuminate\Mail\Mailable;
use Illuminate\Mail\Mailables\Content;
use Illuminate\Mail\Mailables\Envelope;

final class UndanganMasukMail extends Mailable
{
    public function __construct(
        public readonly Undangan $undangan,
        public readonly string $urlSetujui,
        public readonly string $urlTolak,
    ) {}

    public function envelope(): Envelope
    {
        $judul = $this->undangan->ruangan?->pameran?->judul ?? 'Pameran Virtual';

        return new Envelope(
            subject: "Permintaan Diskusi Privat — {$judul}",
        );
    }

    public function content(): Content
    {
        return new Content(view: 'mail.undangan-masuk');
    }
}
