<!DOCTYPE html>
<html lang="id">
<head><meta charset="UTF-8"><title>Permintaan Diskusi Privat</title></head>
<body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background:#f5f5f8; color:#18181b; margin:0; padding:24px;">
  <div style="max-width:520px; margin:0 auto; background:#ffffff; border:1px solid #e6e6ec; border-radius:14px; padding:28px;">
    <p style="margin:0 0 8px; font-size:13px; font-weight:700; color:#ba18f5; letter-spacing:1px;">VEN · DISKUSI PRIVAT</p>
    <h2 style="margin:0 0 12px; font-size:20px; font-weight:700;">Permintaan Undangan Masuk</h2>
    <p style="margin:0 0 18px; font-size:14px; color:#52525b; line-height:1.6;">
      <strong>{{ $undangan->pengguna?->nama ?? 'Pengguna' }}</strong> mengajukan sesi diskusi privat pada pameran
      <strong>{{ $undangan->ruangan?->pameran?->judul ?? 'Pameran 3D' }}</strong>.
    </p>
    <div style="background:#f5f5f8; border-radius:10px; padding:14px 16px; margin-bottom:20px; font-size:14px; line-height:1.7;">
      <div><strong>Jadwal:</strong> {{ $undangan->waktu?->format('d M Y, H:i') }}</div>
      <div><strong>Durasi:</strong> {{ $undangan->durasi_menit ?? 30 }} menit</div>
      <div><strong>Topik / Catatan:</strong> {{ $undangan->catatan ?: '-' }}</div>
    </div>
    <p style="margin:0 0 14px; font-size:13px; color:#71717a;">Anda dapat menyetujui atau menolak langsung lewat tombol di bawah ini, atau melalui tab Jadwal di aplikasi VEN:</p>
    <div style="display:flex; gap:12px;">
      <a href="{{ $urlSetujui }}" style="display:inline-block; background:#ba18f5; color:#ffffff; text-decoration:none; font-weight:600; font-size:14px; padding:11px 20px; border-radius:10px;">Setujui</a>
      <a href="{{ $urlTolak }}" style="display:inline-block; background:#ffffff; color:#dc2626; border:1px solid #e6e6ec; text-decoration:none; font-weight:600; font-size:14px; padding:11px 20px; border-radius:10px;">Tolak</a>
    </div>
  </div>
</body>
</html>
