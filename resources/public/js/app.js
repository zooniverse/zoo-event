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

  var drawPoints = function(error, classifications) {
    var points = classifications.map(function(c) {
      return projection([c.location.result.longitude, 
                        c.location.result.latitude]);
    });

    var group = svg.append('g')
      .attr('class', 'classifications')

    var dots = group.selectAll('circle')
      .data(points)

    dots.enter().append('circle')
      .attr('cx', function(d) { return d[0]; })
      .attr('cy', function(d) { return d[1]; })
      .attr('r', 2)
      .style('fill', 'red');
  };

  d3.json("/classifications", drawPoints);
  setInterval(function() { d3.json('/classifications/10', drawPoints) }, 7500);

}).call(this);
