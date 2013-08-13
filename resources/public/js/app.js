(function() {
  var width = window.innerWidth * .95,
    height = window.innerHeight * .95;

  var projection = d3.geo.mercator()
    .scale((width + 1) / 2 / Math.PI)
    .translate([width / 2, height / 2])
    .precision(.1);

  var path = d3.geo.path()
    .projection(projection);

  var graticule = d3.geo.graticule();

  var svg = d3.select('svg')
    .attr('height', height)
    .attr('width', width);

  svg.append('path')
    .datum(graticule)
    .attr('class', 'graticule')
    .attr('d', path)

  d3.json("vendor/world-50m.json", function(error, world) {
    svg.insert('path', '.graticule')
      .datum(topojson.feature(world, world.objects.land))
      .attr('class', 'land')
      .attr('d', path);

    svg.insert("path", ".graticule")
      .datum(topojson.mesh(world, world.objects.countries, 
                           function(a, b) { return a !== b; }))
      .attr("class", "boundary")
      .attr("d", path); 
  });

  var group = svg.append('g')
    .attr('class', 'classifications')

  var points = []

  var updatePoints = function(error, classifications) {
    if (error) {
      clearInterval(fetcher);
      clearInterval(drawer);
      throw error;
    }
   
    points = points.concat(classifications.map(function(c) {
      var latlng = projection([c.location.longitude, 
                               c.location.latitude]);
      return latlng.concat(c.project);
    }));
  };

  var drawingPoints = [];

  var drawPoints = function() {
    if (drawingPoints.length === 1000)
      drawingpoint.shift();

    drawingPoints.push(points.pop());

    var dots = group.selectAll('circle')
      .data(drawingPoints)

    dots.enter().append('circle')
      .attr('cx', function(d) { return d[0]; })
      .attr('cy', function(d) { return d[1]; })
      .attr('class', function(d) { return d[2]; })
      .attr('r', 1)
      .transition().duration(200)
      .attr('r', 7)
      .transition().duration(500)
      .attr('r', 3);

    dots.exit().remove();
  };

  d3.json("/classifications/9", updatePoints);
  var fetcher = setInterval(function() { d3.json('/classifications/9', updatePoints) }, 5000);
  var drawer = setInterval(drawPoints, 500);

}).call(this);
