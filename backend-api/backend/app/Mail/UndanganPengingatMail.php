<?php

namespace App\Mail;

use App\Models\Undangan;
use Illuminate\Mail\Mailable;
use Illuminate\Mail\Mailables\Content;
use Illuminate\Mail\Mailables\Envelope;

final class UndanganPengingatMail extends Mailable
{
    public function __construct(
        public readonly Undangan $undangan,
        public readonly string $namaPenerima,
        public readonly ?string $deepLink = null,
    ) {}

    public function envelope(): Envelope
    {
        $judul = $this->undangan->ruangan?->pameran?->judul ?? 'Pameran Virtual';

        return new Envelope(
            subject: "Pengingat H-1 Diskusi Privat — {$judul}",
        );
    }

    public function content(): Content
    {
        return new Content(view: 'mail.undangan-pengingat');
    }
}
