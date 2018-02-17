import re
import models


class Transform(object):
  """Abstraction for a regular expression transform.

  Transform subclasses have two properties:
     regexp: the regular expression defining what will be replaced
     replace(MatchObject): returns a string replacement for a regexp match

  We iterate over all matches for that regular expression, calling replace()
  on the match to determine what text should replace the matched text.

  The Transform class is more expressive than regular expression replacement
  because the replace() method can execute arbitrary code to, e.g., look
  up a WikiWord to see if the page exists before determining if the WikiWord
  should be a link.
  """
  def run(self, content):
    """Runs this transform over the given content.

    Args:
      content: The string data to apply a transformation to.

    Returns:
      A new string that is the result of this transform.
    """
    parts = []
    offset = 0
    for match in self.regexp.finditer(content):
      parts.append(content[offset:match.start(0)])
      parts.append(self.replace(match))
      offset = match.end(0)
    parts.append(content[offset:])
    return ''.join(parts)


class WikiWords(Transform):
  """Translates WikiWords to links.

  We look up all words, and we only link those words that currently exist.
  """
  def __init__(self):
    self.regexp = re.compile(r'[A-Z][a-z]+([A-Z][a-z]+)+')

  def replace(self, match):
    wikiword = match.group(0)
    if models.Page.exists(wikiword):
      return '<a class="wikiword" href="/%s">%s</a>' % (wikiword, wikiword)
    else:
      return wikiword


class AutoLink(Transform):
  """A transform that auto-links URLs."""
  def __init__(self):
    self.regexp = re.compile(r'([^"])\b((http|https)://[^ \t\n\r<>\(\)&"]+' \
                             r'[^ \t\n\r<>\(\)&"\.])')

  def replace(self, match):
    url = match.group(2)
    return match.group(1) + '<a class="autourl" href="%s">%s</a>' % (url, url)


class HideReferers(Transform):
  """A transform that hides referers for external hyperlinks."""

  def __init__(self):
    self.regexp = re.compile(r'href="(http[^"]+)"')

  def replace(self, match):
    url = match.group(1)
    scheme, host, path, parameters, query, fragment = urlparse.urlparse(url)
    url = 'http://www.google.com/url?sa=D&amp;q=' + urllib.quote(url)
    return 'href="%s"' % (url,)

