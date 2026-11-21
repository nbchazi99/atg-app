import 'package:flutter/material.dart';

import '../api.dart';
import '../app_state.dart';
import '../theme.dart';
import '../widgets.dart';

class PasswordResetScreen extends StatefulWidget {
  const PasswordResetScreen({super.key, this.initialEmail = ''});

  final String initialEmail;

  @override
  State<PasswordResetScreen> createState() => _PasswordResetScreenState();
}

class _PasswordResetScreenState extends State<PasswordResetScreen> {
  final _email = TextEditingController();
  final _code = TextEditingController();
  final _password = TextEditingController();

  bool _codeSent = false;
  bool _loading = false;
  String? _emailError;
  String? _codeError;
  String? _passwordError;

  @override
  void initState() {
    super.initState();
    _email.text = widget.initialEmail;
  }

  @override
  void dispose() {
    _email.dispose();
    _code.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _sendCode() async {
    final email = _email.text.trim();
    setState(() {
      _emailError = email.isEmpty ? 'Enter your email address' : null;
    });
    if (_emailError != null || _loading) return;

    setState(() => _loading = true);
    try {
      await api.requestPasswordReset(email);
      if (!mounted) return;
      setState(() => _codeSent = true);
      await showMessage(
        context,
        'Check your email',
        'If an account exists for this address, a reset code has been sent. '
        'The code expires in 15 minutes.',
      );
    } on ApiException catch (error) {
      if (mounted) {
        await showMessage(context, 'Could not send reset code', error.message);
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _resetPassword() async {
    setState(() {
      _codeError = RegExp(r'^\d{8}$').hasMatch(_code.text.trim())
          ? null
          : 'Enter the 8-digit code from your email';
      _passwordError = _password.text.length >= 6
          ? null
          : 'Password must be at least 6 characters';
    });
    if (_codeError != null || _passwordError != null || _loading) return;

    setState(() => _loading = true);
    try {
      await api.resetPassword(
        email: _email.text.trim(),
        code: _code.text.trim(),
        password: _password.text,
      );
      if (!mounted) return;
      _password.clear();
      await showMessage(
        context,
        'Password updated',
        'You can now sign in with your new password.',
      );
      if (mounted) Navigator.of(context).pop();
    } on ApiException catch (error) {
      if (mounted) {
        await showMessage(context, 'Could not reset password', error.message);
      }
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AtgColors.bg,
      appBar: AppBar(title: const Text('Password reset')),
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 540),
            child: SingleChildScrollView(
              padding: const EdgeInsets.fromLTRB(20, 20, 20, 32),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    _codeSent ? 'Enter your reset code.' : 'Forgot your password?',
                    style: AtgText.pageTitle.copyWith(fontSize: 28),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    _codeSent
                        ? 'Enter the 8-digit code and choose a new password.'
                        : 'We will email a one-time code if this address belongs to an ATG account.',
                    style: AtgText.bodySmall.copyWith(color: AtgColors.muted),
                  ),
                  const SizedBox(height: 20),
                  AtgField(
                    controller: _email,
                    label: 'Email address',
                    errorText: _emailError,
                    enabled: !_codeSent && !_loading,
                    keyboardType: TextInputType.emailAddress,
                    autofillHints: const [AutofillHints.email],
                  ),
                  if (_codeSent) ...[
                    const SizedBox(height: 12),
                    AtgField(
                      controller: _code,
                      label: '8-digit reset code',
                      errorText: _codeError,
                      enabled: !_loading,
                      keyboardType: TextInputType.number,
                    ),
                    const SizedBox(height: 12),
                    AtgField(
                      controller: _password,
                      label: 'New password',
                      errorText: _passwordError,
                      enabled: !_loading,
                      obscureText: true,
                      autofillHints: const [AutofillHints.newPassword],
                    ),
                    Align(
                      alignment: Alignment.centerRight,
                      child: TextButton(
                        onPressed: _loading ? null : _sendCode,
                        child: const Text('Send another code'),
                      ),
                    ),
                  ],
                  const SizedBox(height: 12),
                  PrimaryButton(
                    label: _codeSent ? 'Reset password' : 'Send reset code',
                    loading: _loading,
                    onPressed: _codeSent ? _resetPassword : _sendCode,
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
