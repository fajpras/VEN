<?php

namespace App\Http\Requests\Pengguna;

use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;

class TanggapiUndanganRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    public function rules(): array
    {
        return [
            'status' => ['required', Rule::in(['disetujui', 'ditolak'])],
            'alasan' => ['nullable', 'string', 'max:255'],
            'catatan' => ['nullable', 'string', 'max:255'],
        ];
    }
}
