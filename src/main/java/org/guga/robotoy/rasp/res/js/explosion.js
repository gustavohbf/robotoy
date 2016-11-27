/*!
 RoboToy Explosion Animations
 ©2016 https://github.com/gustavohbf/robotoy
*/
/**
 * Functions used in explosion animations.
 *
 * Depends on:
 * driving_view.js
 * sprites.js
 */
 
var ROBOTOY = ROBOTOY || {}

ROBOTOY.checkFinishedLoading = ROBOTOY.checkFinishedLoading || (function(){}); 

ROBOTOY.EXPLOSIONS = { 

	explosion : new Array(),
	sprites_explosion_done : false,
	
	bigexplosion : new Array(),
	sprites_bigexplosion_done : false,
	
	animating_damage : false
	
};

ROBOTOY.EXPLOSIONS.loadExplosionSprites = function() {

	var e = ROBOTOY.EXPLOSIONS;

	//e.explosion = loadSeveralImages(0,126,4,'/explosion/explosion','.png');
	loadSpriteOfImages('/explosion.png',126,128,128,11,e.explosion,(function(){
		e.sprites_explosion_done = true;
		ROBOTOY.checkFinishedLoading();
		}));
	
	//e.bigexplosion = loadSeveralImages(0,238,4,'/bigexplosion/bigexplosion','.png');
	loadSpriteOfImages('/bigexplosion.png',238,128,128,15,e.bigexplosion,(function(){
		e.sprites_bigexplosion_done = true;
		ROBOTOY.checkFinishedLoading();
		}));	
}

ROBOTOY.EXPLOSIONS.ExplosionAnimation = function(imgs,x,y,w,h) {
	this.imgs = imgs;
	this.x = x;
	this.y = y;
	this.w = w;
	this.h = h;
	this.count = 0;
	var self = this;
	
	var container = ROBOTOY.VIEW.container;
	this.layer_explosion = new CanvasLayers.Layer(0, 0, container.getWidth(), container.getHeight());
	container.getChildren().add(this.layer_explosion);
	this.layer_explosion.lowerToBottom();
	this.layer_explosion.onRender = function(layer,rect,ctx) {
		var c = self.count;
		if (c==self.imgs.length)
			return;
		if (!self.imgs[c])
			return;
		if (self.imgs[c].render) {
			self.imgs[c].render(ctx,self.x,self.y,self.w,self.h);
		}
		else if (self.imgs[c].src) {
			ctx.drawImage(self.imgs[c],self.x,self.y,self.w,self.h);
		}
	}	
	
	this.animate = function() {
		if (self.count==self.imgs.length) {
			clearInterval(self.explosion_timer)
			self.layer_explosion.close();
			container.markRectsDamaged(container.getRect())
		}
		else {
			self.count++;
			container.markRectsDamaged();
		}
		container.redraw();
	}
	this.explosion_timer = setInterval(this.animate,5)	
};

ROBOTOY.EXPLOSIONS.animateExplosion = function() {
	var e = ROBOTOY.EXPLOSIONS;
	var canvas = ROBOTOY.VIEW.canvas;
	new e.ExplosionAnimation(e.explosion,canvas.width*10/100,canvas.height*10/100,canvas.width*80/100,canvas.height*80/100)
}

ROBOTOY.EXPLOSIONS.animateBigExplosion = function() {
	var e = ROBOTOY.EXPLOSIONS;
	var canvas = ROBOTOY.VIEW.canvas;
	new e.ExplosionAnimation(e.bigexplosion,canvas.width*10/100,canvas.height*10/100,canvas.width*80/100,canvas.height*80/100)
}

ROBOTOY.EXPLOSIONS.flickDamage = function() {
	var e = ROBOTOY.EXPLOSIONS;
	if (e.animating_damage)
		return;
	e.animating_damage = true;
	var container = ROBOTOY.VIEW.container;
	this.layer_damage = new CanvasLayers.Layer(0, 0, container.getWidth(), container.getHeight());
	container.getChildren().add(this.layer_damage);
	this.layer_damage.lowerToBottom();
	this.damage_alpha = 0;
	this.direction_alpha = 1;
	var self = this;
	this.layer_damage.onRender = function(layer,rect,ctx) {
		ctx.fillStyle = 'rgba(250,10,10,'+self.damage_alpha+')';
		ctx.fillRect(0, 0, layer.getWidth(), layer.getHeight());
	}
	this.animate = function() {
		var next_alpha = self.damage_alpha;
		next_alpha += 0.1 * self.direction_alpha;
		if (next_alpha>0.9) {
			self.direction_alpha = -1;
		}
		else if (next_alpha<0.1) {
			self.direction_alpha = 1;
		}
		self.damage_alpha = next_alpha;
		container.markRectsDamaged();
		container.redraw();
	}
	this.damage_timer = setInterval(this.animate,15)
	setTimeout((function(){
		clearInterval(self.damage_timer)
		self.layer_damage.close();
		container.markRectsDamaged(container.getRect())
		container.redraw();
		e.animating_damage = false;
	}),900)
}
