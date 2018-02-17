import webapp2
import os
import jinja2
import string
import re
import time
from google.appengine.api import memcache
from google.appengine.ext import db

template_dir = os.path.join(os.path.dirname(__file__), 'templates')
jinja_env = jinja2.Environment(loader = jinja2.FileSystemLoader(template_dir), autoescape = True)

class Handler(webapp2.RequestHandler):
	def write(self, *a, **kw):
		self.response.out.write(*a, **kw)

	def render_str(self, template, **params):
		t = jinja_env.get_template(template)
		return t.render(params)

	def render_str_escaped(self, template, **params):
		t = jinja_env_escaped.get_template(template)
		return t.render(params)

	def render(self, template, **kw):
		self.write(self.render_str(template, **kw))

	def render_content(self, template, **kw):
		content = self.render_str(template, **kw)
		self.render("index.html", content=content, user=self.get_logged_in_user(), **kw)

	# method to see if a User is logged in or not
	def is_logged_in(self):
		user_id = None
		user = None
		user_id_str = self.request.cookies.get("user_id")
		if user_id_str:
			user_id = check_secure_val(user_id_str)
		return user_id

	# method that returns the actual logged in User
	def get_logged_in_user(self):
		user_id = self.is_logged_in()
		user = None
		if user_id:
			user = User.get_by_id(long(user_id))
		return user

# Model object representing a Wiki Page
class Page(db.Model):
	content = db.TextProperty(required = True)
	title = db.StringProperty(required = True, indexed=True)
	version = db.IntegerProperty(required = True, indexed=True)
	created = db.DateTimeProperty(auto_now_add = True)
	
	

# Model object representing a User of the Wiki
class User(db.Model):
	username = db.StringProperty(required = True)
	password = db.StringProperty(required = True)
	email = db.StringProperty(required = False)
	created = db.DateTimeProperty(auto_now_add = True)

# Handler for logging out of the Wiki
#
# Also clears out the user_id cookie
class LogoutHandler(Handler):
	def get(self):
		self.response.headers.add_header('Set-Cookie', 'user_id=; Path=/')
		self.redirect("/blog/login")


class LoginHandler(Handler):
	
	def get(self):
        	self.render_content("login.html")
	
        
    	def post(self):
        	username = self.request.get("username")
        	userpassword = self.request.get("password")
        			
			
        	u = User.gql("WHERE username = '%s'"%username).get()
                
		if u and valid_pw(username, userpassword, u.password):
        	   uid= str(make_secure_cookie(str(u.key().id())))
        	   self.response.headers.add_header("Set-Cookie", "user_id=%s; Path=/" %uid)
		   self.redirect('/blog')          
				
        	else:
        	    msg = "Invalid login"
        	    self.render_content("login.html", error = msg)	
	    

# Handler for new user signups
class SingupHandler(Handler):
    def get(self):
	 self.render_content("signup.html")	

    def post(self):
        have_error    =	False
        user_name     = self.request.get("username")
        user_password = self.request.get("password")
        user_verify   = self.request.get("verify")
        user_email    = self.request.get("email")

        
        name_error = password_error = verify_error = email_error = ""

        if not valid_username(user_name):
            name_error = "That's not a valid username"
	    have_error = True

        if not valid_password(user_password):
            password_error = "That's not a valid password"
            have_error = True 

        elif user_password != user_verify:
            verify_error = "Your passwords didn't match"
            have_error = True

        if not valid_email(user_email):
            email_error = "That's not a valid email"
            have_error = True
  
        if have_error:
		
		self.render_content("signup.html"
				, username=user_name
				, username_error=name_error
				, password_error=password_error
				, verify_error=verify_error
				, email=user_email
				, email_error=email_error)
        else:      
           
      	   
      	   u = User.gql("WHERE username = '%s'"%user_name).get()
           	   
           if u:
            	name_error = "That user already exists."
            	self.render_content("signup.html",username_error = name_error)
           else:
            # make salted password hash
            	h = make_pw_hash(user_name, user_password)
		u = User(username=user_name, password=h,email=user_email)
		
            	u.put()
                uid= str(make_secure_cookie(str(u.key().id()))) #dis is how we get the id from google data store(gapp engine)
		#The Set-Cookie header which is add_header method will set the cookie name user_id(value,hash(value)) to its value
		self.response.headers.add_header("Set-Cookie", "user_id=%s; Path=/" %uid)
		self.redirect("/blog")


# Handler for a specific Wiki Page Entry
class WikiPageHandler(Handler):
	def get(self, title, args):
		
		if title == None:
			title = '/'

		# get the version query param if available
		version = self.request.get('v')

		# retrieve the Page from the Database
		page = load_page(title, version)

		if not page:
			self.redirect("/blog/_edit%s" % title)
		else:
			# calculate seconds since last cache miss
			seconds = int((time.time())) - page[1]
			# render straight html using templates
			self.render_content("page.html", page=page[0], seconds=seconds)

# Handler for posting new Wiki Page Entries
class EditWikiPageHandler(Handler):
	def render_edit_page(self, title, content=""):
		if self.is_logged_in():
			page = load_page(title)
			self.render_content("edit_page.html", page=page[0] if page else None)
		else:
			self.redirect("/blog/login")

	def get(self, title, args):
		if self.is_logged_in():
			self.render_edit_page(title = title)
		else:
			self.redirect("/blog/login")

	# When submitting new wiki page entries, the content 
	# field is required. If the wiki page is valid, persist 
	# the entry to the DB and redirect to the permalink.
	def post(self, title, args):
		if self.is_logged_in():
			content = self.request.get("content")
			page = load_page(title)
								
		   	if page:
				page = Page(title=title, content=content, version=page[0].version+1)
			else:
				page = Page(title=title, content=content, version=0)

			page.put()
			load_page(title, update = True)
			self.redirect("/blog%s" % title)
		    
		else:
			self.redirect("/blog/login")

# Handler for the History page 
#
# This handler simply displays the history of a Wiki Page
# while providing the ability to edit and view a particular 
# page version
class HistoryWikiPageHandler(Handler):
	def get(self, title, args):
		if title == None:
			title = '/'

		# retrieve the Page Versions from the Database
		pageVersions = db.GqlQuery("SELECT * FROM Page WHERE title = :1", title)
		# display all of the different versions
		self.render_content("history.html", versions=list(pageVersions))

#flush handler
class FlushHandler(Handler):	
	def get(self):
		memcache.flush_all()
		self.redirect("/blog")
		

#Home Page handler
class Home(Handler):
  	 def get(self):
  	      self.redirect("/blog")


# regular expression for handling arbitrary page names
PAGE_RE = r'((/(?:[a-zA-Z0-9_-]+/?)*))?'
# All WebApp Handlers
app = webapp2.WSGIApplication([
		# handler for editing wiki pages
		('/blog/_edit' + PAGE_RE, EditWikiPageHandler),('/', Home)
		# handler for showing the history of wiki pages
		, ('/blog/_history' + PAGE_RE, HistoryWikiPageHandler),('/blog/flush', FlushHandler)
		# handler for logging in
		, ('/blog/login', LoginHandler)
		# handler for logging out
		, ('/blog/logout', LogoutHandler)
		# handler for signup
		, ('/blog/signup', SingupHandler)
		# handler for showing wiki pages
		, ('/blog' + PAGE_RE, WikiPageHandler)
	], debug=True)

