/*!
 RoboToy Sprites Utilities
 ©2016 https://github.com/gustavohbf/robotoy
*/
/**
 * Some utility functions used for loading images used in animation<BR>
 * There are two types of methods implemented here:<BR>
 * - Loading several images individually (function 'loadSeveralImages')<BR>
 * - Loading one big image that contains several images (function 'loadSpriteOfImages')<BR>
 *
 * Author: Gustavo Figueiredo
 */
 
/**
 * Load up to 'length' images referenced by a uri formed
 * according to the following pattern:<BR>
 * /path/to/image/somename-000123.png<BR>
 * first = first number of image filenames (e.g. 0)<BR>
 * length = total number of images<BR>
 * digits = number of digits for numbering images (e.g. 5 for 00123)<BR>
 * uri_prefix = common name of each image (e.g. "/path/to/image/somename-")<BR>
 * ext = filename extension including dot (e.g. ".png")
 */
function loadSeveralImages(first,length,digits,uri_prefix,ext,onComplete) {
	var zeroes = "0".repeat(digits);
	var imgs = [];
	for (var i=0;i<length;i++) {
		var sufix = (zeroes + (i + first)).slice(-digits);
		var img_name = uri_prefix+sufix+ext;
		var img = new Image();
		img.src = img_name;
		imgs.push(img)
	}
	if (onComplete) {
		onComplete.call();
	}
	return imgs;
}

/**
 * Represents a slice of a bigger image.<BR>
 * full_img = full image<BR>
 * x = x-offset in full image for a particular slice<BR>
 * y = y-offset in full image for a particular slice<BR>
 * w = width of a particular slice<BR>
 * h = height of a particular slice<BR>
 * render = function that can be used for painting the slice image
 * over a given canvas context<BR>
 */
var SliceRenderer = function(full_img,x,y,w,h) {
	this.full_img = full_img;
	this.x = x;
	this.y = y;
	this.w = w;
	this.h = h;
	var self = this;
	this.render = function(ctx,x,y,w,h) {
		ctx.drawImage(self.full_img,self.x,self.y,self.w,self.h,x,y,w,h);
	};
};

/**
 * Load a big image consisting of several smaller images (sprites).<BR>
 * uri = address of the big image<BR>
 * length = number of smaller images inside the big image<BR>
 * imgwidth = width of each smaller image<BR>
 * imgheight = height of each smaller image<BR>
 * cols = number of smaller images that fits in one big image width<BR>
 * output_array_of_renderers = array that will receive several instances of 'SliceRenderer' object, one
 * for each smaller image<BR>
 */
function loadSpriteOfImages(uri,length,imgwidth,imgheight,cols,output_array_of_renderers,onComplete) {
	var full_img = new Image();
	full_img.onload = function() {
		for (var i=0;i<length;i++) {
			var x = (i%cols)*imgwidth;
			var y = Math.floor(i/cols)*imgheight;
			output_array_of_renderers.push(new SliceRenderer(full_img,x,y,imgwidth,imgheight))
		}
		if (onComplete) {
			onComplete.call();
		}
	};
	full_img.src = uri;
}
