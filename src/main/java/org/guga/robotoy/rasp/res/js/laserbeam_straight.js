/*!
 RoboToy Laser Beam Animation
 ©2016 https://github.com/gustavohbf/robotoy
*/
/**
 * Functions used in animating a laser beam as a series of three straight lines.
 *
 * Depends on:
 * driving_view.js
 * canvaslayers.js
 * vector.js
 */

var ROBOTOY = ROBOTOY || {}

ROBOTOY.VIEW = ROBOTOY.VIEW || {}

ROBOTOY.LASER = {
		
	AnimationMode : {
		FROM_BELOW : 0,
		FROM_ABOVE : 1,
		FROM_TURRETS_ABOVE : 2
	},
	
	animating_beam : false,

	LaserBeam : function() {
		var mode = ROBOTOY.LASER.animation_mode;
		var container = ROBOTOY.VIEW.container;
		var AnimationMode = ROBOTOY.LASER.AnimationMode;
		var dash_len = 0.5; // beam length
		var pause_len = dash_len/2; // space between beams
		
		var fov = 45; // degrees
		var aspect = container.getWidth() / container.getHeight();
		var near = 0.1;
		var far = 1000;

		var y_offset;
		var x_offset;
		if (mode==AnimationMode.FROM_ABOVE) { 
			y_offset = 10;
			x_offset = 0;
		}
		else if (mode==AnimationMode.FROM_TURRETS_ABOVE) {
			y_offset = 10;
			if (ROBOTOY.LASER.turrent_left_shot) {
				x_offset = -10;
				ROBOTOY.LASER.turrent_left_shot = false;
			}
			else {
				x_offset = 10;
				ROBOTOY.LASER.turrent_left_shot = true;
			}
		}
		else if (mode==AnimationMode.FROM_BELOW) {
			y_offset = -10;
			x_offset = 0;
		}
		var max_distance = 10;

		var tempMatrix = new Matrix();
		Matrix.identity(tempMatrix);
		Matrix.perspective(fov, aspect, near, far, tempMatrix);
		
		this.num_dashes = 3;
		this.beam_thickness = 4.0;
		this.glow_thickness = this.beam_thickness/5; 
		this.blur_thickness = 16.0; // pixels 
		this.increment = 0.1; // increment in z-axis per frame
		var frame_tick_delay = 5; // ms per frame
		var self = this;
		this.layer_ray = new CanvasLayers.Layer(0, 0, container.getWidth(), container.getHeight());
		container.getChildren().insert(this.layer_ray);
		
		var Dash = function(distance,dash_len,thickness) {

			var dash_coordinates = 
			[
				new Vector(-thickness + x_offset,y_offset,distance+0.1),
				new Vector(thickness + x_offset,y_offset,distance+0.1),
				new Vector(thickness + x_offset,y_offset,(distance-dash_len)+0.1),
				new Vector(-thickness + x_offset,y_offset,(distance-dash_len)+0.1),
			];
			
			this.distance = distance;
			this.finished = false;
			this.dash_len = dash_len;
			var self = this;
			this.move = function(increment) {
				if (self.finished)
					return;
				self.distance += increment;
				if ((self.distance-self.dash_len>max_distance)) {
					self.finished = true;
					return;
				}
				dash_coordinates[0].z += increment;
				dash_coordinates[1].z += increment;
				dash_coordinates[2].z += increment;
				dash_coordinates[3].z += increment;
			};
			this.render = function(ctx,thickness,xc) {
				if (self.finished)
					return false; // finished this animation
				
				if (dash_coordinates[0].z<=near)
					return false; // out of sight
				var v1_projected = tempMatrix.transformPoint(dash_coordinates[0].clip_z(near))
				var v2_projected = tempMatrix.transformPoint(dash_coordinates[1].clip_z(near))
				var v3_projected = tempMatrix.transformPoint(dash_coordinates[2].clip_z(near))
				var v4_projected = tempMatrix.transformPoint(dash_coordinates[3].clip_z(near))
				
				ctx.beginPath();
				ctx.moveTo(v1_projected.x, v1_projected.y);
				ctx.lineTo(v2_projected.x, v2_projected.y);
				ctx.lineTo(v3_projected.x, v3_projected.y);
				ctx.lineTo(v4_projected.x, v4_projected.y);
				ctx.lineTo(v1_projected.x, v1_projected.y);
				ctx.closePath();
				
				return true;
			}
		}
		this.rays = new Array();
		for (var i=0;i<this.num_dashes;i++) {
		
			var distance = -i*(dash_len+pause_len);
			var dash = new Dash(distance,dash_len,self.beam_thickness);
			this.rays.push(dash);
		}
		this.layer_ray.onRender = function(layer,rect,ctx) {
			try {
				var x0 = container.getWidth()/2-self.beam_thickness-self.blur_thickness;
				var y0 = container.getHeight()/2;
				var xc = self.beam_thickness+self.blur_thickness;
				ctx.save();
				ctx.rect(x0,y0,
					(self.beam_thickness+self.blur_thickness)*2,
					container.getHeight()/2)
				ctx.clip();
				ctx.translate(x0,y0);
				ctx.globalCompositeOperation = "lighter";
				ctx.globalAlpha = 1.0;
				ctx.shadowBlur = self.blur_thickness;
				ctx.shadowColor = ctx.fillStyle = "rgb(255,50,50)";
				
				self.rays.forEach(function(ray){
					if (!ray.finished) {
						var rendered = ray.render(ctx,self.beam_thickness,xc);
						if (rendered)
							ctx.fill();
					}
				});
				
				ctx.shadowColor = ctx.strokeStyle = "rgb(255,120,120)";
	
				self.rays.forEach(function(ray){
					if (!ray.finished) {
						var rendered = ray.render(ctx,self.glow_thickness,xc);
						if (rendered)
							ctx.fill();
					}
				});
				
				ctx.restore();
			}
			catch (error) { 
				console.log(error)
			}
		};
		this.laser_timer = setInterval((function(){
			var all_finished = true;
			self.rays.forEach(function(ray){
				if (!ray.finished) {
					all_finished = false;
					ray.move(self.increment);
				}
			});
			if (all_finished) {
				ROBOTOY.LASER.animating_beam = false;
				clearInterval(self.laser_timer);
				self.layer_ray.close();
			}
			container.markRectsDamaged();
			container.redraw();		
		}),frame_tick_delay);
	}

};

ROBOTOY.LASER.animation_mode = ROBOTOY.LASER.AnimationMode.FROM_TURRETS_ABOVE;

ROBOTOY.LASER.animateBeam = function() {
	var l = ROBOTOY.LASER;
	if (l.animating_beam)
		return;
	l.animating_beam = true;
	new l.LaserBeam();
}