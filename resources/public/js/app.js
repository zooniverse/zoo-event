(function() {
  var width = window.innerWidth,
    height = window.innerHeight;

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

  var drawPoints = function(error, classifications) {
    points = points.concat(classifications.map(function(c) {
      return projection([c.location.result.longitude, 
                         c.location.result.latitude,
                         c.project]);
    })).slice(0, 100);

    var dots = group.selectAll('circle')
      .data(points)

    dots.enter().append('circle')
      .attr('cx', function(d) { return d[0]; })
      .attr('cy', function(d) { return d[1]; })
      .attr('class', function(d) { return d[2]; })
      .attr('r', 1)
      .transition().duration(700)
      .attr('r', 7)
      .transition().duration(500)
      .attr('r', 3);

    dots.exit().remove();
  };

  //d3.json("/classifications", drawPoints);
  setInterval(function() { d3.json('/classifications/1', drawPoints) }, 1000);

}).call(this);
