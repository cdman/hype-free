var iframeModifier = {
	closedText : '[+] show source code',
	openedText : '[-] hide source code',
	
	addEvent : function (obj, evType, fn) {
		//from http://onlinetools.org/articles/unobtrusivejavascript/chapter4.html
		if (obj.addEventListener){ 
			obj.addEventListener(evType, fn, false); 
			return true; 
		} else if (obj.attachEvent){ 
			var r = obj.attachEvent("on"+evType, fn); 
			return r; 
		} else { 
			return false; 
		} 
	},
	
	handleClick : function (e) {
		//from http://www.quirksmode.org/js/events_properties.html
		var targ;
		if (!e) var e = window.event;
		if (e.target) targ = e.target;
		else if (e.srcElement) targ = e.srcElement;
		if (targ.nodeType == 3) // defeat Safari bug
			targ = targ.parentNode;
		
		var iframeTarg = targ;
		while ( null != iframeTarg && iframeTarg.tagName.toLowerCase() != 'iframe' ) 
			iframeTarg = iframeTarg.nextSibling;
		if (null == iframeTarg) return false;
		
		if ('none' == iframeTarg.style.display) {
			targ.innerHTML = iframeModifier.openedText;
			iframeTarg.style.display = '';
		} else {
			targ.innerHTML = iframeModifier.closedText;
			iframeTarg.style.display = 'none';
		}
		
		return true;
	},
	
	loadInit : function () {
		var iframes = document.getElementsByTagName('iframe');
		for (var i = 0; i < iframes.length; i++) {
			if (iframes[i].className.indexOf('source_code') > -1) {
				var clickTarget = document.createElement('a');
				clickTarget.innerHTML = iframeModifier.closedText;
				clickTarget.target = "#";
				clickTarget.className = 'source_code_display';
				iframes[i].parentNode.insertBefore(clickTarget, iframes[i]);
				clickTarget.onclick = iframeModifier.handleClick;
				iframes[i].style.display = 'none';					
			}
		}
	},	
};

var quoteResolver = {
  addEvent : function (obj, evType, fn) {
		//taken from: http://www.scottandrew.com/weblog/articles/cbs-events 
    if (obj.addEventListener){ 
      obj.addEventListener(evType, fn, false); 
      return true; 
    } else if (obj.attachEvent){ 
      var r = obj.attachEvent("on"+evType, fn); 
      return r; 
    } else { 
      return false; 
		}		
  },
	
	doWork: function () {
		//add a " before and after each q
		var qs = document.getElementsByTagName('q');
  	for (var i = 0; i < qs.length; i++) {
  		var before = document.createTextNode('"');
  		var after = document.createTextNode('"');
  		qs[i].parentNode.insertBefore(before, qs[i]);
  		qs[i].parentNode.insertBefore(after, qs[i].nextSibling); 
  	}
		
		//deactivate the font-style: italic rule			
		for (var i = 0; i < document.styleSheets.length; i++) {
			//the standard would be cssRules, but IE uses rules
			//and we are targeting IE only
			var ruleList = document.styleSheets[i].rules;
			for (var j = 0; j < ruleList.length; j++) 
				if ('Q' == ruleList[j].selectorText && 'italic' == ruleList[j].style.fontStyle) {
					//this is the style we wish to disable
					ruleList[j].style.fontStyle = '';
					break; 					
				} 
		}									
	},
	
	init : function () {
		//try to determine if this is an IE browser
		var userAgent = /MSIE/; var nonUserAgent = /Opera/; var os = /Windows/;
		if ( userAgent.exec(navigator.userAgent) && !nonUserAgent.exec(navigator.userAgent) && os.exec(navigator.userAgent) ) {  
			//register a function to do the work after we finish loading
			this.addEvent(window, 'load', this.doWork);
		}
	}
};


$(document).ready(function() {
  var posts = $('div.post');
  var post_index = 0;
  
  var quoteResolverInit = function() {
		//try to determine if this is an IE browser
		var userAgent = /MSIE/; var nonUserAgent = /Opera/; var os = /Windows/;
		if ( userAgent.exec(navigator.userAgent) && !nonUserAgent.exec(navigator.userAgent) && os.exec(navigator.userAgent) ) {  
			quoteResolver.doWork();
		}    
  }
  
  var iframeModifierInit = function() {
    iframeModifier.loadInit();
    setTimeout(quoteResolverInit, 100);
  }
  
  var process_post = function() {
    if (post_index >= posts.length) {
      setTimeout(iframeModifierInit, 100);
      return;
    }
    var e = posts.get(post_index);
    var titleElement = $('.post-title', e);    
    var postBodyText = $('.post-body', e).html();
    
    //remove <code> and <pre> entirely
    postBodyText = postBodyText.replace(/<\s*(?:code|pre)[\s\S]*?\/(?:code|pre)[\s\S]*?>/gi, '');
    //strip the other tags
    postBodyText = postBodyText.replace(/<[\s\S]*?>/gi, '');
    if (postBodyText.length < 100) return;
    
    //escape " and limit length
    var title = titleElement.text().substring(0, 40).replace(/"/g, '&quot;');   
    postBodyText = postBodyText.substring(0, 10000).replace(/"/g, '&quot;');
    
    $('a', titleElement).after(
      '<form name="dataForm" method="post" target = "_blank" action="http://wordscount.info/hw/service/smog/analyze.jsp">'
      + '<input type="hidden" name="document_title" value="' + title + '" />'
      + '<input type="hidden" name="user_text" value="' + postBodyText + '" />'
      + '<input type="hidden" name="service_id" value="WordsCount">'
      + '<input type="hidden" name="service_name" value="SMOG Calculator">'
      + '<input class="smog-button" type="submit" value="SMOG" />'
      + '</form>');
    
    post_index += 1;
    setTimeout(process_post, 100);
  };  
  
  if (!document.cookie.match(/hypefreevisited/)) {
    $('#first-time-visitor-greeting').css('display', '');
    document.cookie = 'hypefreevisited=1; expires=Thu, 2 Aug 2099 20:47:11 UTC; path=/';
  }

  setTimeout(process_post, 100);
});