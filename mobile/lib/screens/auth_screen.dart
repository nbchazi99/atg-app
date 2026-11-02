import 'package:flutter/material.dart';

import '../api.dart';
import '../app_state.dart';
import '../theme.dart';
import '../widgets.dart';

class AuthScreen extends StatefulWidget {
  const AuthScreen({super.key});

  @override
  State<AuthScreen> createState() => _AuthScreenState();
}

class _AuthScreenState extends State<AuthScreen> {
  final _name = TextEditingController();
  final _phone = TextEditingController();
  final _email = TextEditingController();
  final _password = TextEditingController();

  bool _register = false;
  bool _loading = false;
  String? _nameError;
  String? _phoneError;
  String? _emailError;
  String? _passwordError;

  @override
  void dispose() {
    _name.dispose();
    _phone.dispose();
    _email.dispose();
    _password.dispose();
    super.dispose();
  }

  void _setMode(bool register) {
    setState(() {
      _register = register;
      _nameError = null;
      _phoneError = null;
      _emailError = null;
      _passwordError = null;
    });
  }

  Future<void> _submit() async {
    if (_loading) return;
    setState(() {
      _nameError = null;
      _phoneError = null;
      _emailError = null;
      _passwordError = null;
    });

    final email = _email.text.trim();
    final password = _password.text;
    final name = _name.text.trim();
    final phone = _phone.text.trim();

    var invalid = false;
    if (email.isEmpty) {
      _emailError = 'Enter your email address';
      invalid = true;
    }
    if (password.length < 6) {
      _passwordError = 'Password must be at least 6 characters';
      invalid = true;
    }
    if (_register) {
      if (name.isEmpty) {
        _nameError = 'Enter your full name';
        invalid = true;
      }
      if (phone.isEmpty) {
        _phoneError = 'Enter your mobile number';
        invalid = true;
      }
    }
    if (invalid) {
      setState(() {});
      return;
    }

    setState(() => _loading = true);
    try {
      final result = _register
          ? await api.register(name, email, phone, password)
          : await api.login(email, password);
      await session.saveAuth(result.token, result.user);
    } on ApiException catch (e) {
      if (!mounted) return;
      await showMessage(
        context,
        _register ? 'Could not create the account' : 'Could not sign you in',
        e.message,
      );
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _fillDemo({required bool admin}) {
    _setMode(false);
    _email.text = admin ? 'admin@atg.app' : 'blessed@atg.app';
    _password.text = admin ? 'Admin@123' : 'User@123';
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 540),
            child: SingleChildScrollView(
              padding: const EdgeInsets.fromLTRB(20, 24, 20, 32),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Row(
                    children: [
                      AtgLogo(size: 52),
                      SizedBox(width: 12),
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'ATG',
                            style: TextStyle(
                              fontSize: 20,
                              fontWeight: FontWeight.w800,
                              letterSpacing: 1.2,
                              color: AtgColors.ink,
                            ),
                          ),
                          SizedBox(height: 2),
                          Text(
                            'ACCURATE TRADING GROUP',
                            style: TextStyle(
                              fontFamily: 'monospace',
                              fontFamilyFallback: ['Menlo', 'Courier New'],
                              fontSize: 9,
                              letterSpacing: 1.1,
                              fontWeight: FontWeight.w600,
                              color: AtgColors.gold,
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                  const SizedBox(height: 28),
                  Text(
                    _register ? 'Join the floor.' : 'Welcome back.',
                    style: AtgText.pageTitle.copyWith(fontSize: 30),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    _register
                        ? 'Create your ATG account to track courses, calls and payment requests.'
                        : 'Sign in to your ATG account. Your courses, calls and wallet live here.',
                    style: AtgText.bodySmall.copyWith(color: AtgColors.muted),
                  ),
                  const SizedBox(height: 20),
                  AtgChipRow(
                    labels: const ['Sign in', 'Create account'],
                    selected: _register ? 1 : 0,
                    onSelected: (index) => _setMode(index == 1),
                  ),
                  if (_register) ...[
                    const SizedBox(height: 18),
                    AtgField(
                      controller: _name,
                      label: 'Full name',
                      errorText: _nameError,
                      textCapitalization: TextCapitalization.words,
                      autofillHints: const [AutofillHints.name],
                    ),
                    const SizedBox(height: 12),
                    AtgField(
                      controller: _phone,
                      label: 'Mobile number',
                      errorText: _phoneError,
                      keyboardType: TextInputType.phone,
                      autofillHints: const [AutofillHints.telephoneNumber],
                    ),
                  ],
                  const SizedBox(height: 12),
                  AtgField(
                    controller: _email,
                    label: 'Email address',
                    errorText: _emailError,
                    keyboardType: TextInputType.emailAddress,
                    autofillHints: const [AutofillHints.email],
                  ),
                  const SizedBox(height: 12),
                  AtgField(
                    controller: _password,
                    label: 'Password',
                    errorText: _passwordError,
                    obscureText: true,
                    autofillHints: const [AutofillHints.password],
                    onSubmitted: (_) => _submit(),
                  ),
                  const SizedBox(height: 20),
                  PrimaryButton(
                    label: _register ? 'Create account' : 'Sign in',
                    loading: _loading,
                    onPressed: _submit,
                  ),
                  const SizedBox(height: 24),
                  SurfaceCard(
                    padding: const EdgeInsets.all(14),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const SectionLabel('DEMO ACCOUNTS'),
                        const SizedBox(height: 6),
                        Text(
                          'Learner: blessed@atg.app / User@123\nAdministrator: admin@atg.app / Admin@123',
                          style: AtgText.small.copyWith(
                            color: AtgColors.inkSoft,
                            fontSize: 12,
                          ),
                        ),
                        const SizedBox(height: 4),
                        Row(
                          children: [
                            TextButton(
                              onPressed: () => _fillDemo(admin: false),
                              style: TextButton.styleFrom(
                                padding: EdgeInsets.zero,
                                minimumSize: const Size(0, 36),
                                tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                              ),
                              child: const Text(
                                'Fill learner details',
                                style: TextStyle(
                                  fontSize: 12.5,
                                  fontWeight: FontWeight.w700,
                                  color: AtgColors.gold,
                                ),
                              ),
                            ),
                            const SizedBox(width: 16),
                            TextButton(
                              onPressed: () => _fillDemo(admin: true),
                              style: TextButton.styleFrom(
                                padding: EdgeInsets.zero,
                                minimumSize: const Size(0, 36),
                                tapTargetSize: MaterialTapTargetSize.shrinkWrap,
                              ),
                              child: const Text(
                                'Fill admin details',
                                style: TextStyle(
                                  fontSize: 12.5,
                                  fontWeight: FontWeight.w700,
                                  color: AtgColors.gold,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 16),
                  Center(
                    child: TextButton(
                      onPressed: () async {
                        await showServerDialog(context);
                        if (mounted) setState(() {});
                      },
                      child: Text(
                        'Server: ${session.server}',
                        textAlign: TextAlign.center,
                        style: const TextStyle(
                          fontSize: 11.5,
                          color: AtgColors.gold,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(height: 4),
                  const Center(
                    child: Text(
                      'Trading involves risk. ATG is an education and signals platform, not a broker.',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 10.5,
                        color: AtgColors.muted,
                        height: 1.4,
                      ),
                    ),
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
