/*!
 RoboToy Lobby Room Robots View
 ©2016 https://github.com/gustavohbf/robotoy
*/
/**
 * Functions used for showing all robots during lobby room section.
 *
 */

var ROBOTOY = ROBOTOY || {}

ROBOTOY.COMM = ROBOTOY.COMM || {}

ROBOTOY.ROBOTS = {

	robots : new Array(),
	
	robots_row : null,
	
	Robot : function(id,address,owner,robot_seq,robot_color) {
		var r = ROBOTOY.ROBOTS;
		this.id = id;
	    this.address = address;
	    this.owner = owner;
	    this.seq = robot_seq;
	    
		this.div = document.getElementById('robot').cloneNode(true);
	    this.div.style.display = "inline";
	    this.div.style.position = "relative";
	    this.div.id = "robot_"+robot_seq; 
	    
	    this.f_address = this.div.children["robot_address"];
	    this.f_address.name = "robot_"+robot_seq+"_address";
	    this.f_address.value = address;
	    this.f_owner = this.div.children["robot_owner"];
	    this.f_owner.id = "robot_"+robot_seq+"_owner";
	    this.f_choose_btn = this.div.children["robot_choose"];
	    this.f_choose_btn.name = "robot_"+robot_seq+"_choose";
	    this.f_choose_btn.id = "robot_"+robot_seq+"_choose";
	    this.f_color = this.div.children["robot_color"];
	    this.f_color.name = "robot_"+robot_seq+"_color";
	    this.f_color.id = "robot_"+robot_seq+"_color";
	    if (robot_color)
	 		this.f_color.style.background = robot_color;
	    
	    this.setOwner = function(owner) {
	    	this.owner = owner;
		    if (owner) {
		    	this.f_owner.innerHTML = escapeHtml(owner);
		    	var username = document.getElementById("username");
		    	if (username && owner==username.value) {
		    		this.f_choose_btn.value = "Leave"; 
			    	this.f_choose_btn.disabled = false;
			    	this.f_choose_btn.onclick = function(e) { r.leaveRobot(id) };
		    	}
		    	else {
		    		this.f_choose_btn.value = "Owned"; 
		    		this.f_choose_btn.disabled = true;
		    	}
		    }
		    else {    	
		    	this.f_owner.innerHTML = "";
		    	this.f_choose_btn.value = "Choose";
		    	this.f_choose_btn.disabled = false;
		    	this.f_choose_btn.onclick = function(e) { r.takeRobot(id) };
		    }
	    };
	    this.setColor = function(robot_color) {
	    	this.f_color.style.background = robot_color;
	    };
	    this.setOwner(owner);
	}
	
};

ROBOTOY.ROBOTS.addRobot = function(id,addr,owner,robot_color) {
	var row = ROBOTOY.ROBOTS.robots_row;
	if (!row)
		return;
	var cell = row.insertCell(-1);
    var robot_seq = row.cells.length;
    var r = ROBOTOY.ROBOTS;
    
    var new_robot = new r.Robot(id,addr,owner,robot_seq,robot_color);
    r.robots.push(new_robot);
    
	cell.appendChild(new_robot.div);
}

ROBOTOY.ROBOTS.takeRobot = function(id) {
	if (ROBOTOY.game_ready)
		return;
	var c = ROBOTOY.COMM;
	if (!c.has_connection) {
		window.alert('not connected!');
		return;
	}
	try {
		c.connection.send('T'+id);
	}
	catch (error) {
		window.alert(error);
	}
}

ROBOTOY.ROBOTS.leaveRobot = function(id) {
	if (ROBOTOY.game_ready)
		return;
	var c = ROBOTOY.COMM;
	if (!c.has_connection) {
		window.alert('not connected!');
		return;
	}
	try {
		c.connection.send('L'+id);
	}
	catch (error) {
		window.alert(error);
	}
}

ROBOTOY.ROBOTS.getRobotOwned = function() {
	var robot_owned = null; 
	var username = ROBOTOY.username;
    var r = ROBOTOY.ROBOTS;
	for (var i=0;i<r.robots.length;i++) {
		var robot = r.robots[i];
		if (robot.owner==username) {
			robot_owned = robot;
			break;
		}
	}
	return robot_owned;
}
