<?php

namespace App\Http\Requests\Profil;

use App\Http\Requests\Concerns\MenormalkanEmail;
use Illuminate\Foundation\Http\FormRequest;

final class GantiEmailRequest extends FormRequest
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
            'otp' => ['required', 'digits:'.config('otp.panjang')],
        ];
    }

    public function messages(): array
    {
        return [
            'email_baru.required' => __('api.profile.email_required'),
            'email_baru.email' => __('api.profile.email_invalid'),
            'email_baru.unique' => __('api.profile.email_taken'),
            'otp.required' => __('api.otp.invalid'),
            'otp.digits' => __('api.otp.invalid'),
        ];
    }
}
