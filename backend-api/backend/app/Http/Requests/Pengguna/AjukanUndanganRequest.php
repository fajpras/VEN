<?php

namespace App\Http\Requests\Pengguna;

use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;

class AjukanUndanganRequest extends FormRequest
{
    public function authorize(): bool
    {
        return true;
    }

    public function rules(): array
    {
        return [
            'id_ruangan' => ['nullable', 'integer', Rule::exists('ruangan', 'id_ruangan')->where('jenis', 'privat')],
            'id_pameran' => ['nullable', 'integer', 'exists:pameran,id_pameran'],
            'id_karya' => ['nullable', 'string', 'exists:karya,id_karya'],
            'waktu' => ['required', 'date', 'after:now'],
            'catatan' => ['nullable', 'string', 'max:255'],
        ];
    }
}
