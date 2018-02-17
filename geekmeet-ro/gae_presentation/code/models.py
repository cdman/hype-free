from google.appengine.ext import db

import transforms


class Page(db.Model):
  name = db.StringProperty(required = True, indexed=True)
  content = db.TextProperty()
  created = db.DateTimeProperty(auto_now_add = True)
  modified = db.DateTimeProperty(auto_now = True)
  user = db.UserProperty()

  def edit_url(self):
    return '/%s?mode=edit' % (self.name,)

  def view_url(self):
    return '/' + self.name

  def wikified_content(self):
    """Applies our wiki transforms to our content for HTML display.

    We auto-link URLs, link WikiWords, and hide referers on links that
    go outside of the Wiki.
    
    Returns:
      The wikified version of the page contents.
    """
    transform_collection = [
      transforms.AutoLink(),
      transforms.WikiWords(),
      transforms.HideReferers(),
    ]
    content = self.content
    for transform in transform_collection:
      content = transform.run(content)
    return content

  @staticmethod
  def load(name):
    """Loads the page with the given name.

    Returns:
      We always return a Page instance, even if the given name isn't yet in
      the database. In that case, the Page object will be created when save()
      is called.
    """
    page = db.Query(Page).filter('name =', name).get()
    if page:
      return page
    else:
      return Page(name = name)

  @staticmethod
  def exists(name):
    """Returns true if the page with the given name exists in the datastore."""
    return bool(db.Query(Page).filter('name =', name).get(keys_only = True))

