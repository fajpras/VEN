<!DOCTYPE html>
<html lang="id">
<head><meta charset="UTF-8"><title>Pengingat H-1 Diskusi Privat</title></head>
<body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background:#f5f5f8; color:#18181b; margin:0; padding:24px;">
  <div style="max-width:520px; margin:0 auto; background:#ffffff; border:1px solid #e6e6ec; border-radius:14px; padding:28px;">
    <p style="margin:0 0 8px; font-size:13px; font-weight:700; color:#ba18f5; letter-spacing:1px;">VEN · PENGINGAT H-1</p>
    <h2 style="margin:0 0 12px; font-size:20px; font-weight:700;">Jadwal Diskusi Privat Besok</h2>
    <p style="margin:0 0 16px; font-size:14px; color:#52525b; line-height:1.6;">
      Halo <strong>{{ $namaPenerima }}</strong>, ini adalah pengingat bahwa sesi diskusi privat pada pameran
      <strong>{{ $undangan->ruangan?->pameran?->judul ?? 'Pameran 3D' }}</strong> akan berlangsung besok.
    </p>
    <div style="background:#f5f5f8; border-radius:10px; padding:14px 16px; margin-bottom:20px; font-size:14px; line-height:1.7;">
      <div><strong>Jadwal:</strong> {{ $undangan->waktu?->format('d M Y, H:i') }} ({{ $undangan->durasi_menit ?? 30 }} menit)</div>
      <div><strong>Topik:</strong> {{ $undangan->catatan ?: '-' }}</div>
      <div><strong>Tautan aktif sampai:</strong> 00:00 malam hari jadwal</div>
    </div>
    @if($deepLink)
      <a href="{{ $deepLink }}" style="display:inline-block; background:#ba18f5; color:#ffffff; text-decoration:none; font-weight:600; font-size:14px; padding:11px 20px; border-radius:10px;">Buka di Aplikasi VEN</a>
    @endif
  </div>
</body>
</html>
