<!DOCTYPE html>
<html lang="id">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>VEN — Tanggapan Undangan</title>
</head>
<body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background:#f5f5f8; color:#18181b; margin:0; padding:32px 16px;">
  <div style="max-width:460px; margin:40px auto; background:#ffffff; border:1px solid #e6e6ec; border-radius:16px; padding:28px; text-align:center;">
    <p style="margin:0 0 8px; font-size:13px; font-weight:700; color:#ba18f5; letter-spacing:1.2px;">VEN</p>
    <h1 style="margin:0 0 12px; font-size:20px; font-weight:700;">{{ $judul }}</h1>
    <p style="margin:0 0 16px; font-size:14px; color:#52525b; line-height:1.6;">{{ $pesan }}</p>
    @if(!empty($deepLink))
      <a href="{{ $deepLink }}" style="display:inline-block; background:#ba18f5; color:#ffffff; text-decoration:none; font-weight:600; font-size:14px; padding:11px 22px; border-radius:10px;">Buka Aplikasi VEN</a>
    @endif
  </div>
</body>
</html>
