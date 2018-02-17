#!/usr/bin/env python
#
# Copyright 2008 Google Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

"""A simple Google App Engine wiki application."""

__author__ = 'Bret Taylor'

import os
import urllib
import urlparse
import webapp2

from google.appengine.api import users
from google.appengine.ext.webapp import template

import models


# Set to true if we want to have our webapp print stack traces, etc
_DEBUG = True


class BaseRequestHandler(webapp2.RequestHandler):
  """Supplies a common template generation function.

  When you call generate(), we augment the template variables supplied with
  the current user in the 'user' variable and the current webapp request
  in the 'request' variable.
  """
  
  def render(self, template_name, template_values={}):
    values = {
      'request': self.request,
      'user': users.get_current_user(),
      'login_url': users.create_login_url(self.request.uri),
      'logout_url': users.create_logout_url(self.request.uri),
      'application_name': 'Wiki',
    }
    values.update(template_values)
    directory = os.path.dirname(__file__)
    path = os.path.join(directory, os.path.join('templates', template_name))
    self.response.out.write(template.render(path, values, debug=_DEBUG))


class WikiPage(BaseRequestHandler):
  """Our one and only request handler.

  We first determine which page we are editing, using "MainPage" if no
  page is specified in the URI. We then determine the mode we are in (view
  or edit), choosing "view" by default.

  POST requests to this handler handle edit operations, writing the new page
  to the datastore.
  """
  def get(self, page_name):
    """Handle HTTP GET requests throughout the application, used to present
    the view and edit modes of wiki pages.
    
    Args:
      page_name: The wikified name of the current page.
    """
    # Load the main page by default
    if not page_name:
      page_name = 'MainPage'
    page = models.Page.load(page_name)

    # Default to edit for pages that do not yet exist
    if not page.is_saved():
      mode = 'edit'
    else:
      modes = ['view', 'edit']
      mode = self.request.get('mode')
      if not mode in modes:
        mode = 'view'

    # User must be logged in to edit
    if mode == 'edit' and not users.get_current_user():
      self.redirect(users.create_login_url(self.request.uri))
      return

    # Genertate the appropriate template
    self.render(mode + '.html', {
      'page': page,
    })

  def post(self, page_name):
    """Handle HTTP POST requests throughout the application, used to handle
    posting new or edited wiki pages.
    
    Args:
      page_name: The wikified name of the current page.
    """
    # User must be logged in to edit
    if not users.get_current_user():
      # The GET version of this URI is just the view/edit mode, which is a
      # reasonable thing to redirect to
      self.redirect(users.create_login_url(self.request.uri))
      return

    # We need an explicit page name for editing
    if not page_name:
      self.redirect('/')

    # Create or overwrite the page
    page = models.Page.load(page_name)
    page.content = self.request.get('content')
    page.save()
    self.redirect(page.view_url())


app = webapp2.WSGIApplication([('/(.*)', WikiPage)], debug=_DEBUG)

