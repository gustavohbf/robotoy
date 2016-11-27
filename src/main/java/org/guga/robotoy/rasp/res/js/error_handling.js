/*!
 RoboToy Error Handling
 ©2016 https://github.com/gustavohbf/robotoy
*/
/**
 * Redirects all local exceptions to server.
 *
 * Depends on:
 * jquery
 */

(function(){
    var oldLog = console.log;
    /*console.log = function (message) {
    
		try {
			var console_msg = {
				type: 'POST',
				url: '/info',
				data: JSON.stringify({context: navigator.userAgent, message: message}),
				contentType: 'application/json; charset=utf-8'
			};
			$.ajax(console_msg)
			return true;
		}
		catch (error) {
		}
    
        oldLog.apply(console, arguments);
    };*/
	window.onerror = function(message, source, lineno, colno, error) {
		try {
			var error_msg = {
				type: 'POST',
				url: '/error',
				data: JSON.stringify({context: navigator.userAgent, lineno: lineno, source: source, message: message}),
				contentType: 'application/json; charset=utf-8'
			};
			$.ajax(error_msg)
			return true;
		}
		catch (error) {
			oldLog.log(error)
			return false;
		}
	}

})();
