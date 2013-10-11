(function() {
  var countryFetcher, fetcher, pointsDrawer, userDrawer, width, height, namesDrawer, cpmDrawer;
  var names = {};

  width = window.innerWidth - 400;
  height = Math.floor(width * (9 / 16));

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
        fill: "url(#img)",
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
      .html(function(d) { return d[0] + ': <span class="count"></span>'; });

    list.select('.count').text(function(d) { return d[1].toLocaleString() });

    list.exit().remove();
  }

  var update = function(error, classifications) {
    points = points.concat(classifications.map(function(c) {
      return projection([
        c.location.longitude, 
        c.location.latitude
      ]).concat([c.project, c.id]);
    }));

    users = users.concat(classifications.map(function(c) {
      if (typeof names[c.user] !== 'undefined')
        names[c.user] = names[c.user] + 1;

      var subject = new Image;
      subject.src = c.subject;

      return {
        id: c.id,
        subject: c.subject,
        country: c.location.country,
        city: c.location.city,
        project: c.project
      }
    }));

    users = users.reduce(function(m, u) {
      if (typeof u.user === 'undefined') {
        u.user = 'not-logged-in';
        return m.concat(u);
      } else if (m.filter(function(mu) { return mu.id === u.id; }).length === 0) {
        u.user = u.user.split('@')[0];
        return m.concat(u);
      } else {
        return m;
      }
    }, []);
  };

  var drawingPoints = [];

  var drawNames = function() {
    var namesForList = []
    for(var key in names) {
      namesForList.push([key, names[key]]);
    }

    namesForList = namesForList.sort(function(l, r) { 
      if (l[1] < r[1])
        return 1;
      else if (l[1] > r[1])
        return -1;
      return 0;
    });

    d3.select('.top-tables ul').selectAll('li').remove()

    var list = d3.select('.top-tables ul').selectAll('li')
      .data(namesForList, function(d) { return d[0]; });

    list.enter().append('li')
      .text(function(d) { return d[0]; });
  };

  var drawCPM = function() {
    d3.json("/cpm", function(response) {
      d3.select('.cpm h1').text(response.value);
      cpmDrawer = setTimeout(drawCPM, 10000);
    });
  };

  var drawPoints = function(interval) {
    return function() {
      interval = interval || 500;
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

      if (dots.length > 0) 
        pointsDrawer = setTimeout(drawPoints(interval), interval);
    };
  };

  var drawUsers = function(setInterval, hideImg) {
    var drawUser = function(d) {
      return '<h2>' + d.city + ((d.city) ? ", " : "") + d.country + '</h2>' +
        '<img src="' + d.subject + '"></div>';
      };

    return function() {
      var interval = Math.max(8000 * (1 / users.length), 250);
      var transition = (setInterval || interval) * 0.75;

      var scale = d3.scale.linear().domain([0, 7])
        .range([-150, window.innerWidth + 150])
      var width = scale(1) - scale(0) - 20,
        height = Math.floor(width / 2);

      d3.select("#bottom").style('bottom', height + 20 + 'px');

      var classifiers = d3.select('.classifiers').selectAll('span')
        .data(users.slice(0, 7), function(d) { return d.id; });

      classifiers.enter().append('span')
        .attr('class', function(d) { return "classification " + d.project; })
        .style('width', width + 'px')
        .style('height', height + 'px')
        .style('left', function(d, i) { return scale(i + 1) + 'px'; })
        .html(drawUser);

      classifiers.transition().duration(transition)
        .style('left', function(d, i) { return scale(i) + "px"; });

      classifiers.exit()
        .transition().duration(transition)
        .style('left', function(d, i) { return scale(i - 1) + "px"; })
        .remove();

      if (users.length > 7)
        users.shift();

      if (userDrawer) 
        clearTimeout(userDrawer);

      userDrawer = setTimeout(drawUsers(setInterval, hideImg), (setInterval || interval));
    };
  };

  drawTime = function() {
    var timeEl = document.querySelector('.time h1');
    timeEl.innerHTML = moment().format('lll');
    setTimeout(drawTime, 60000);
  };

  if (location.hash === "") {
    document.querySelector('.top-tables').setAttribute('style', 'display: none;');
  } else {
    location.hash.slice(2).split(',').forEach(function (n) { names[n] = 0; });
    namesDrawer = setInterval(drawNames, 2000);
  }
  d3.json("/classifications/13", update);
  d3.json("/countries", updateCountries);
  countryFetcher = setInterval(function() {d3.json("/countries", updateCountries);}, 10000);
  fetcher = setInterval(function() { d3.json('/classifications/13', update) }, 2000);
  pointsDrawer = setTimeout(drawPoints(), 500);
  userDrawer = setTimeout(drawUsers(), 1000);
  drawCPM();
  drawTime();
}).call(this);
