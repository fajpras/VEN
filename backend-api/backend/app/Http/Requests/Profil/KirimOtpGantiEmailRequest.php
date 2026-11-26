<?php

namespace App\Http\Requests\Profil;

use App\Http\Requests\Concerns\MenormalkanEmail;
use Illuminate\Foundation\Http\FormRequest;
use Illuminate\Validation\Rule;

final class KirimOtpGantiEmailRequest extends FormRequest
{
    use MenormalkanEmail;

    public function authorize(): bool
    {
        return true;
    }

    protected function kolomEmail(): array
    {
        return ['email_baru'];
    }

    public function rules(): array
    {
        return [
            'email_baru' => ['required', 'email:rfc', 'max:254', 'unique:pengguna,email'],
            'kata_sandi' => [Rule::requiredIf(fn (): bool => filled($this->user()->kata_sandi)), 'nullable', 'string', 'max:72'],
        ];
    }

    public function messages(): array
    {
        return [
            'email_baru.required' => __('api.profile.email_required'),
            'email_baru.email' => __('api.profile.email_invalid'),
            'email_baru.unique' => __('api.profile.email_taken'),
            'kata_sandi.required' => __('api.profile.password_required'),
        ];
    }
}
