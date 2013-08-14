(function() {
  var width = 1620,
    height = 1030;

  var projection = d3.geo.wagner6()
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

  var points = [];
  var users = [];

  var update = function(error, classifications) {
    if (error) {
      clearInterval(fetcher);
      throw error;
    }
   
    points = points.concat(classifications.map(function(c) {
      return projection([
        c.location.longitude, 
        c.location.latitude
      ]).concat([c.project, c.id]);
    }));

    users = users.concat(classifications.map(function(c) {
      return {
        country: c.location.country,
        city: c.location.city,
        user: c.user,
        avatar: c.user_id,
        project: c.project
      }
    }));

    users = users.reduce(function(m, u) {
      if (m.filter(function(mu) { return mu.avatar === u.avatar; }).length === 0)
        return m.concat(u);
      else
        return m;
    }, []);
  };

  var drawingPoints = [];

  var drawPoints = function() {
    if (drawingPoints.length === 1000)
      drawingPoints.shift();

    drawingPoints.push(points.pop());

    var dots = group.selectAll('circle')
      .data(drawingPoints, function(d) { return d[3]; });

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

  var avatarURI = "http://zooniverse-avatars.s3.amazonaws.com/ouroboros/"
  var usersList = d3.select('.classifiers ul');
  this.defaultAvatar = function(el) {
    var img = document.createElement('img')
    img.src = src="http://zooniverse-avatars.s3.amazonaws.com/default_forum_avatar.png";
    img.height = 50;
    img.width = 50;
    el.parentNode.replaceChild(img, el);
  };

  var drawUsers = function() {
    var classifiers = usersList.selectAll('li')
      .data(users.slice(0, 5), function(d) { return d.avatar; });

    classifiers.enter().append('li')
      .attr('class', 'classifiers')
      .html(drawUser);

    classifiers.exit().remove();

    if (users.length > 5)
      users.shift();
  };

  var drawUser = function(d) {
    return '<img width="50" height="50" src="' + avatarURI + d.avatar + '" onerror="window.defaultAvatar(this)" /> <span><div class="username"> ' + d.user + '</div><div class="location">' + d.city + ', ' + d.country + '</div></span>'
  };

  d3.json("/classifications/99", update);
  var fetcher = setInterval(function() { d3.json('/classifications/9', update) }, 5000);
  var pointsDrawer = setInterval(drawPoints, 500);
  var userDrawer = setInterval(drawUsers, 2000); 

}).call(this);
