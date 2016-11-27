/*! lightgl.js
 * ©2011-2016 Evan Wallace - https://github.com/evanw/lightgl.js/
 */
/**
 * Functions and structures used for projecting 3D objects into
 * 2D view.
 */

/**
 * Simple vector representation and just a few vector operations
 */
function Vector(x, y, z) {
	this.x = x || 0;
	this.y = y || 0;
	this.z = z || 0;
}

Vector.prototype = {
		divide: function(v) {
			if (v instanceof Vector) return new Vector(this.x / v.x, this.y / v.y, this.z / v.z);
			else return new Vector(this.x / v, this.y / v, this.z / v);
		},
		clip_z: function(min) {
			if (this.z>=min)
				return this;
			else
				return new Vector(this.x, this.y, min);
		}
}

var hasFloat32Array = (typeof Float32Array != 'undefined');

/**
 * Simple matrix representation and just a few matrix operations
 */
function Matrix() {
	var m = Array.prototype.concat.apply([], arguments);
	if (!m.length) {
		m = [
		     1, 0, 0, 0,
		     0, 1, 0, 0,
		     0, 0, 1, 0,
		     0, 0, 0, 1
		     ];
	}
	this.m = hasFloat32Array ? new Float32Array(m) : m;
}

Matrix.prototype = {
		multiply: function(matrix) {
			return Matrix.multiply(this, matrix, new Matrix());
		},
		transformPoint: function(v) {
			var m = this.m;
			return new Vector(
					m[0] * v.x + m[1] * v.y + m[2] * v.z + m[3],
					m[4] * v.x + m[5] * v.y + m[6] * v.z + m[7],
					m[8] * v.x + m[9] * v.y + m[10] * v.z + m[11]
			).divide(m[12] * v.x + m[13] * v.y + m[14] * v.z + m[15]);
		}
}

Matrix.identity = function(result) {
	result = result || new Matrix();
	var m = result.m;
	m[0] = m[5] = m[10] = m[15] = 1;
	m[1] = m[2] = m[3] = m[4] = m[6] = m[7] = m[8] = m[9] = m[11] = m[12] = m[13] = m[14] = 0;
	return result;
};

Matrix.multiply = function(left, right, result) {
	result = result || new Matrix();
	var a = left.m, b = right.m, r = result.m;

	r[0] = a[0] * b[0] + a[1] * b[4] + a[2] * b[8] + a[3] * b[12];
	r[1] = a[0] * b[1] + a[1] * b[5] + a[2] * b[9] + a[3] * b[13];
	r[2] = a[0] * b[2] + a[1] * b[6] + a[2] * b[10] + a[3] * b[14];
	r[3] = a[0] * b[3] + a[1] * b[7] + a[2] * b[11] + a[3] * b[15];

	r[4] = a[4] * b[0] + a[5] * b[4] + a[6] * b[8] + a[7] * b[12];
	r[5] = a[4] * b[1] + a[5] * b[5] + a[6] * b[9] + a[7] * b[13];
	r[6] = a[4] * b[2] + a[5] * b[6] + a[6] * b[10] + a[7] * b[14];
	r[7] = a[4] * b[3] + a[5] * b[7] + a[6] * b[11] + a[7] * b[15];

	r[8] = a[8] * b[0] + a[9] * b[4] + a[10] * b[8] + a[11] * b[12];
	r[9] = a[8] * b[1] + a[9] * b[5] + a[10] * b[9] + a[11] * b[13];
	r[10] = a[8] * b[2] + a[9] * b[6] + a[10] * b[10] + a[11] * b[14];
	r[11] = a[8] * b[3] + a[9] * b[7] + a[10] * b[11] + a[11] * b[15];

	r[12] = a[12] * b[0] + a[13] * b[4] + a[14] * b[8] + a[15] * b[12];
	r[13] = a[12] * b[1] + a[13] * b[5] + a[14] * b[9] + a[15] * b[13];
	r[14] = a[12] * b[2] + a[13] * b[6] + a[14] * b[10] + a[15] * b[14];
	r[15] = a[12] * b[3] + a[13] * b[7] + a[14] * b[11] + a[15] * b[15];

	return result;
};

Matrix.perspective = function(fov, aspect, near, far, result) {
	var y = Math.tan(fov * Math.PI / 360) * near;
	var x = y * aspect;
	return Matrix.frustum(-x, x, -y, y, near, far, result);
};

Matrix.frustum = function(l, r, b, t, n, f, result) {
	result = result || new Matrix();
	var m = result.m;

	m[0] = 2 * n / (r - l);
	m[1] = 0;
	m[2] = (r + l) / (r - l);
	m[3] = 0;

	m[4] = 0;
	m[5] = 2 * n / (t - b);
	m[6] = (t + b) / (t - b);
	m[7] = 0;

	m[8] = 0;
	m[9] = 0;
	m[10] = -(f + n) / (f - n);
	m[11] = -2 * f * n / (f - n);

	m[12] = 0;
	m[13] = 0;
	m[14] = -1;
	m[15] = 0;

	return result;
};
