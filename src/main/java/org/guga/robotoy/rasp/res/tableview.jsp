<%@ page contentType="text/html;charset=iso-8859-1" 
	language="java"%><!DOCTYPE html>
<html>
<head>
	<title>${param.title}</title>
	<meta charset="UTF-8">
	<meta name="viewport" content="width=device-width,initial-scale=1.0">
	<meta name="copyright" content="© 2016 https://github.com/gustavohbf/robotoy" />
	<link rel="stylesheet" type="text/css" href="buttons.css" />
	<link rel="stylesheet" type="text/css" href="datatables/jquery.dataTables.css">
	<script src="jquery-1.12.0.min.js"></script>
	<script type="text/javascript" charset="utf8" src="datatables/jquery.dataTables.js"></script>
</head>
<body>
<img src="logo.png" alt="RoboToy" width="200"/> 
<p><h1>${param.title}</h1></p>
  
<table id="results" class="display table table-striped table-bordered" cellspacing="0" width="100%"></table>

<input type="button" class="blue button" value="Back" onclick="window.history.back();">

<script>
$(document).ready(function() {
	
	var get_columns_from_array = function(jsonData) {
		var columns = [];
		var names = {}
		for (var i in jsonData) {
			var el = jsonData[i]
			get_columns(el,columns,names,'')
		}
		return columns;
	}
	
	var get_columns = function(jsonData,columns,names,prefix) {
		for (var key in jsonData) {
			if (!names[key]) {
				var value = jsonData[key];
				if (value && typeof(value)==='object') {
					get_columns(value,columns,names,prefix+key+'.');
				}
				else {
					columns.push({title:prefix+key,data:prefix+key,defaultContent:""});
				}
				names[key] = key;
			}
		}
	};

	$.ajax({
        type: 'GET',
        dataType: 'json',
        url: '${param.url}',
        success: function(d) {        	
        	if (d) {
        		var is_array = d.constructor === Array;
        		if (!is_array) {
        			d = [d];
        		}
                $('#results').DataTable({
                    dom: "Bfrtip",
                    data: d,
                    columns: get_columns_from_array(d)
                });        		
        	}
        }
    });
} );
</script>
</body>
</html>