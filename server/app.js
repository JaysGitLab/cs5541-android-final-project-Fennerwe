var express = require('express');
var mysql = require('mysql');
var bodyParser = require('body-parser');
var fs = require('fs');
var gm = require('gm');

var connection = mysql.createConnection({
	host: 'localhost',
	user: 'pictracker',
	password: 'pictracker',
	database: 'pic_tracker'
});

var app = express();

app.use(bodyParser.urlencoded({extended:true}));
app.use(bodyParser.json());

connection.connect(function(err){
	if(!err) console.log("Database is connected ... \n\n");
	else console.log("Error connecting to database ... \n\n");
});

var port = process.env.PORT || 3030;

var router = express.Router();

router.use(function(req, res, next){
	//console.log('Something is happening.\n');
	next();
});

router.get('/', function(req, res){
	res.json({message: 'api working'});
});

router.route('/pics/:leftlat/:leftlong/:rightlat/:rightlong')
	.get(function(req, res){
		var coords = {
			"left": {
				"lat": req.params.leftlat,
				"long": req.params.leftlong
			},
			"right": {
				"lat": req.params.rightlat,
				"long": req.params.rightlong
			}
		};
		
		var query = "SELECT url, image_id, geo_lat, geo_long \
					 FROM image \
					 WHERE geo_lat < ? \
						   AND geo_lat > ? \
						   AND geo_long < ? \
						   AND geo_long > ?";
					 
		var params = [coords.left.lat,
					  coords.right.lat,
					  coords.right.long,
					  coords.left.long
					 ];
		
		connection.query(query, params, function(err, rows, fields){
			if(err) res.json({success: false});
			
			res.json({success: true, data: rows});
		});
	});
	
router.route('/upload')
	.post(function(req, res){
		var imageString = req.body.image;
		var geo_lat = parseFloat(req.body.lat);
		var geo_long = parseFloat(req.body.long);
		
		var query = "SELECT COUNT(*) as num_images from image";
		connection.query(query, [], function(err, rows, fields){
			if(err) res.json({success: false});
			else {
				var next_image = "image"+(rows[0].num_images+1);
				var next_image_name = '/images/' + next_image + '.jpg';
				var next_image_thumb = '/images/' + next_image + '_thumb.jpg';
				fs.writeFile(next_image_name, imageString, 'base64', function(err){
					if(err) res.json({success: false});
					else {
						gm(next_image_name)
						.resize('2048', '2048')
						.write(next_image_name, function(err){
							if(err) console.log("Error resizing");
						});
						gm(next_image_name)
							.resize('100', '100', "^")
							.gravity('Center')
							.crop('100', '100')
							.write(next_image_thumb, function(err){
								if(err) res.json({success: false});
								else {
									query = "INSERT INTO image(image_id, url, geo_lat, geo_long) VALUES(?, ?, ?, ?)";
									var params = [next_image_name, next_image, geo_lat, geo_long];
									connection.query(query, params, function(err, rows, fields){
										if(err) res.json({success: false});
										else res.json({success: true});
									});
								}
							});
					}
				});
			}
		});
	});
	
app.use('/images', express.static('images'));
app.use('/api', router);

app.listen(port);