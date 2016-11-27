/*!
 RoboToy Audio
 ©2016 https://github.com/gustavohbf/robotoy
*/
/**
 * Functions used in audio playback.
 */

var ROBOTOY = ROBOTOY || {}

ROBOTOY.checkFinishedLoading = ROBOTOY.checkFinishedLoading || (function(){}); 

ROBOTOY.AUDIO = {

	AudioFile : function(audio_params) {
		audio_params.onload = this.finishedLoading.bind(this);
		this.obj = new Howl(audio_params);
		this.done = false;
	},
	
	initialized_array : false,
	
	audio_done : false,

	audio : []
};

ROBOTOY.AUDIO.loadAudioFiles = function() {
	var a = ROBOTOY.AUDIO;
	
	a.audio["laser"] = new a.AudioFile({src:['laser.mp3'],
		sprite: {
			charge:[0,1000],
			blast:[1000,985]
		},autoplay:false,loop:false});
	a.audio["explosion"] = new a.AudioFile({src:['explosion.mp3'],
			autoplay:false,loop:false});
	a.audio["bigexplosion"] = new a.AudioFile({src:['bigexplosion.mp3'],
			autoplay:false,loop:false});
	a.audio["damage"] = new a.AudioFile({src:['damage.mp3'],
			autoplay:false,loop:false});
			
	a.initialized_array = true;
	a.checkFinishedLoadingAudio();
};

ROBOTOY.AUDIO.checkFinishedLoadingAudio = function() {
	var a = ROBOTOY.AUDIO;
	
	if (!a.initialized_array)
		return;
	
	var missing = 0;
	Object.keys(a.audio).forEach(function(key){
		if (!a.audio[key].done) missing++;		
	});
	if (missing==0) {
		a.audio_done = true;
		ROBOTOY.checkFinishedLoading();
	}
};

ROBOTOY.AUDIO.AudioFile.prototype.finishedLoading = function() {
	this.done = true;
	ROBOTOY.AUDIO.checkFinishedLoadingAudio();
}

ROBOTOY.AUDIO.playLaserSound = function() {
	ROBOTOY.AUDIO.playSound("laser","blast")
}

ROBOTOY.AUDIO.playExplosionSound = function() {
	ROBOTOY.AUDIO.playSound("explosion")
}

ROBOTOY.AUDIO.playBigExplosionSound = function() {
	ROBOTOY.AUDIO.playSound("bigexplosion")
}

ROBOTOY.AUDIO.playDamageSound = function() {
	ROBOTOY.AUDIO.playSound("damage")
}

ROBOTOY.AUDIO.playSound = function(name,sprite) {
	var a = ROBOTOY.AUDIO;
	try {
		var sound = a.audio[name].obj
		if (!sound)
			return
		if (sprite)
			sound.play(sprite);
		else
			sound.play();
	}
	catch (error) {
		console.log(error) 
	}
}

