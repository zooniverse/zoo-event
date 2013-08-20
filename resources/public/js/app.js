(function() {
  var width = window.innerWidth - 400,
    height = Math.floor(width * (9 / 16));

  if (width < 480) {
    map = document.getElementById("map");
    map.setAttribute("style", "float: left;");
    width = 480;
    height = Math.floor(width * (9 / 16));
  }

  var projection = d3.geo.wagner6()
    .scale((width + 1) / 2 / Math.PI)
    .translate([width / 2, height / 2])
    .precision(.1);

  var path = d3.geo.path()
    .projection(projection);

  var svg = d3.select('svg')
    .attr('height', height)
    .attr('width', width);

  d3.json("vendor/worldcountries.json", function(error, world) {
    svg.selectAll("path")
      .data(world.features)
      .enter()
      .insert("path", ":first-child")
      .attr({
        d: function(d) {return path(d)},
        id: function(d) {return d.properties.name},
        "class": "land"
      });
     
    svg.select("#Antarctica").remove();
  }); 

  var group = svg.append('g')
    .attr('class', 'classifications')

  var points = [];
  var users = [];

  var updateCountries = function(error, countries) {
    if (error) {
      clearInterval(countryFetcher);
      throw error;
    }

    var list = d3.select('.countries ul').selectAll('li')
      .data(countries.slice(0, 3), function(d) { return d[0]; })

    list.enter().append('li')
      .html(function(d) { return d[0] + ': <span class="count">' + d[1] + '</span>'; });

    list.select('.count').text(function(d) { return d[1] });

    list.exit().remove();
  }

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
        subject: c.subject,
        country: c.location.country,
        city: c.location.city,
        user: c.user,
        avatar: c.user_id,
        project: c.project
      }
    }));

    users = users.reduce(function(m, u) {
      if (typeof u.user === 'undefined')
        return m
      else if (m.filter(function(mu) { return mu.avatar === u.avatar; }).length === 0)
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
    var scale = d3.scale.linear().domain([0, 5]).range([0, window.innerHeight])
    var classifiers = usersList.selectAll('li')
      .data(users.slice(0, 5), function(d) { return d.avatar; });

    classifiers.enter().append('li')
      .attr('class', 'classifiers')
      .style('top', function(d, i) { return scale(i + 1) + 'px'; })
      .html(drawUser);

    classifiers.transition().duration(1900)
      .style('top', function(d, i) { return scale(i) + "px"; });

    classifiers.exit()
      .transition().duration(1900)
      .style('top', function(d, i) { return scale(i - 1) + "px"; })
      .remove();

    if (users.length > 5)
      users.shift();
  };

  var drawUser = function(d) {
    var imgHeight = Math.floor(window.innerHeight / 5);
    return '<div class="image"><img src="' + d.subject + '" width="340" height="' + ((imgHeight > 340) ? '' : imgHeight) + '"></div>' + '<div class="user"><img width="50" height="50" src="' + avatarURI + d.avatar + '" onerror="window.defaultAvatar(this)" /> <span><div class="username"> ' + d.user + '</div><div class="location">' + ((d.city !== '') ? d.city + ', ' : '') + d.country + '</div></span></div>';
  };

  d3.json("/classifications/99", update);
  d3.json("/countries", updateCountries);
  var countryFetcher = setInterval(function() {d3.json("/countries", updateCountries);}, 10000);
  var fetcher = setInterval(function() { d3.json('/classifications/9', update) }, 5000);
  var pointsDrawer = setInterval(drawPoints, 500);
  var userDrawer = setInterval(drawUsers, 2000); 

}).call(this);
