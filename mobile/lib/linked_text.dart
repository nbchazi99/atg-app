import 'package:flutter/gestures.dart';
import 'package:flutter/material.dart';
import 'package:url_launcher/url_launcher.dart';

import 'theme.dart';

final _linkPattern = RegExp(
  r'(?:(?:https?://|www\.)[^\s<>()]+|(?:[A-Za-z0-9-]+\.)+[A-Za-z]{2,}(?::\d{1,5})?(?:/[^\s<>()]*)?)',
  caseSensitive: false,
);

Uri? _webUri(String value) {
  final hasScheme = RegExp(r'^[A-Za-z][A-Za-z0-9+.-]*:').hasMatch(value);
  final normalized = hasScheme ? value : 'https://$value';
  final uri = Uri.tryParse(normalized);
  if (uri == null ||
      (uri.scheme != 'http' && uri.scheme != 'https') ||
      uri.host.isEmpty) {
    return null;
  }
  return uri;
}

Future<bool> openWebLink(String value) async {
  final uri = _webUri(value.trim());
  if (uri == null) return false;
  try {
    return await launchUrl(uri, mode: LaunchMode.externalApplication);
  } on Exception catch (error, stackTrace) {
    debugPrint('Could not open link $uri: $error\n$stackTrace');
    return false;
  }
}

class LinkedText extends StatefulWidget {
  const LinkedText(
    this.text, {
    super.key,
    required this.style,
    this.textAlign,
    this.maxLines,
    this.overflow,
  });

  final String text;
  final TextStyle style;
  final TextAlign? textAlign;
  final int? maxLines;
  final TextOverflow? overflow;

  @override
  State<LinkedText> createState() => _LinkedTextState();
}

class _LinkedTextState extends State<LinkedText> {
  final List<TapGestureRecognizer> _recognizers = [];

  @override
  void didUpdateWidget(covariant LinkedText oldWidget) {
    super.didUpdateWidget(oldWidget);
    _disposeRecognizers();
  }

  @override
  void dispose() {
    _disposeRecognizers();
    super.dispose();
  }

  void _disposeRecognizers() {
    for (final recognizer in _recognizers) {
      recognizer.dispose();
    }
    _recognizers.clear();
  }

  Future<void> _open(String value) async {
    if (!await openWebLink(value) && mounted) {
      ScaffoldMessenger.maybeOf(context)?.showSnackBar(
        const SnackBar(content: Text('Could not open this link')),
      );
    }
  }

  List<InlineSpan> _spans() {
    _disposeRecognizers();
    final spans = <InlineSpan>[];
    var cursor = 0;
    for (final match in _linkPattern.allMatches(widget.text)) {
      final raw = match.group(0)!;
      var linkLength = raw.length;
      while (linkLength > 0 && '.,!?;:'.contains(raw[linkLength - 1])) {
        linkLength--;
      }
      if (linkLength == 0) continue;

      final start = match.start;
      final end = start + linkLength;
      if (start > cursor) {
        spans.add(TextSpan(text: widget.text.substring(cursor, start)));
      }
      final link = raw.substring(0, linkLength);
      final recognizer = TapGestureRecognizer()..onTap = () => _open(link);
      _recognizers.add(recognizer);
      spans.add(
        TextSpan(
          text: link,
          style: const TextStyle(
            color: AtgColors.gold,
            decoration: TextDecoration.underline,
          ),
          recognizer: recognizer,
        ),
      );
      cursor = end;
    }
    if (cursor < widget.text.length) {
      spans.add(TextSpan(text: widget.text.substring(cursor)));
    }
    return spans;
  }

  @override
  Widget build(BuildContext context) {
    return Text.rich(
      TextSpan(style: widget.style, children: _spans()),
      textAlign: widget.textAlign,
      maxLines: widget.maxLines,
      overflow: widget.overflow,
    );
  }
}
