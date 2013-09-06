(function() {
  var countryFetcher, fetcher, pointsDrawer, userDrawer, width, height;

  if (location.hash === "")
    width = window.innerWidth - 400;
  else {
    width = window.innerWidth;
    document.getElementById('map').setAttribute('style', "margin-top: -100px;");
    document.getElementsByClassName('countries')[0].setAttribute('style', "right: 50px;");
  }
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
      var subject = new Image;
      subject.src = c.subject;
      var avatar = new Image;
      if(c.user_id) {
        avatar.src = 'http://zooniverse-avatars.s3.amazonaws.com/ouroboros/' + c.user_id;
      }

      return {
        id: c.id,
        subject: c.subject,
        country: c.location.country,
        city: c.location.city,
        avatar: c.user_id,
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

  var avatarURI = "http://zooniverse-avatars.s3.amazonaws.com/ouroboros/"
  var usersList = d3.select('.classifiers ul');
  this.defaultAvatar = function(el) {
    var img = document.createElement('img')
    img.src = src="http://zooniverse-avatars.s3.amazonaws.com/default_forum_avatar.png";
    img.height = 50;
    img.width = 50;
    el.parentNode.replaceChild(img, el);
  };

  var drawUsers = function(setInterval, hideImg) {
    var drawUser = function(d) {
        var imgHeight = Math.floor(window.innerHeight / 5);
        imgHeight = (imgHeight > 340) ? 340 : imgHeight;
        var avatarSrc = d.user_id ? avatarURI + d.avatar : 'http://zooniverse-avatars.s3.amazonaws.com/default_forum_avatar.png'

        return '<div class="image" style="height: ' + imgHeight + 'px;">' +
                 ((hideImg) ? '' : '<img src="' + d.subject + '" width="340" height="' + imgHeight + '">') +
               '</div>' +
               '<div class="user">' +
                 '<img width="50" height="50" src="' + avatarSrc + '" onerror="window.defaultAvatar(this)" /> ' +
                 '<span>' +
                   '<div class="location">' + ((d.city !== '') ? d.city + ', ' : '') + d.country + '</div>' +
                 '</span>' +
               '</div>';
      };

    return function() {
      var interval = Math.max(8000 * (1 / users.length), 250);
      var transition = (setInterval || interval) * 0.75;

      var scale = d3.scale.linear().domain([0, 5]).range([0, window.innerHeight])
      var classifiers = usersList.selectAll('li')
        .data(users.slice(0, 5), function(d) { return d.id; });

      classifiers.enter().append('li')
        .attr('class', 'classifiers')
        .style('top', function(d, i) { return scale(i + 1) + 'px'; })
        .html(drawUser);

      classifiers.transition().duration(transition)
        .style('top', function(d, i) { return scale(i) + "px"; });

      classifiers.exit()
        .transition().duration(transition)
        .style('top', function(d, i) { return scale(i - 1) + "px"; })
        .remove();

      if (users.length > 5)
        users.shift();

      if (userDrawer) 
        clearTimeout(userDrawer);

      userDrawer = setTimeout(drawUsers(setInterval, hideImg), (setInterval || interval));
    };
  };

    if (location.hash === "") {
    d3.json("/classifications/9", update);
    d3.json("/countries", updateCountries);
    countryFetcher = setInterval(function() {d3.json("/countries", updateCountries);}, 10000);
    fetcher = setInterval(function() { d3.json('/classifications/9', update) }, 2000);
    pointsDrawer = setTimeout(drawPoints(), 500);
    userDrawer = setTimeout(drawUsers(), 1000);
  } else {
    d3.json("/countries", updateCountries);
    d3.json("/classifications/date/" + location.hash.slice(1), function(error, result) {
       points = points.concat(result.map(function(c) {
          return projection([
            c.location.longitude, 
            c.location.latitude
          ]).concat([c.project, c.id]);
        }));
      location.hash = "#ready";
      var ev = document.addEventListener('keypress', function() {
        document.removeEventListener('keypress', ev);
        drawPoints(10)();
      }, true);
    });
  }

}).call(this);
