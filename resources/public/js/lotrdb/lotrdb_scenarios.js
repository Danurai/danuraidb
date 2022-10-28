var _scenarios;
var _scen;

var piedata = {
  labels: ['Enemy', 'Location', 'Treachery'], //objectives
  datasets: [{
    backgroundColor: ['rgba(128,0,0,0.2)','rgba(0,128,0,0.2)','rgba(0,0,128,0.2)']
  }]   
}
var pieoptions = {
  legend: {
    position: "bottom"
  },
  title: {
    display: true,
    text: "Encounter Deck Card Ratios",
    fontFamily: "'Eczar', serif",
    fontSize: 18
  }
}
var bardata = {
  labels: ['Surge','Shadow'],
  datasets: [{
    label: 'In Deck',
    backgroundColor: ['rgba(128,0,0,0.2)','rgba(0,128,0,0.2)'],
    borderColor: ['rgba(128,128,128,0.6)','rgba(128,128,128,0.6)'],
    borderWidth: 1
  },{
    label: 'Other cards',
    backgroundColor: ['rgba(0,0,0,0)','rgba(0,0,0,0)'],
    borderColor: ['rgba(128,128,128,0.6)','rgba(128,128,128,0.6)'],
    borderWidth: 1
  }]
}
var baroptions = {
  scales: {
    xAxes: [{
      stacked: true
    }],
    yAxes: [{
      stacked: true
    }]
  },
  legend: {
    position: "bottom"
  },
  title: {
    display: true,
    text: "Surge and Treachery card counts",
    fontFamily: "'Eczar', serif",
    fontSize: 18
  }
}
var bardiff_data = {
  labels: ['FFG', 'TftC'],
  datasets: [
    {
      label: 'Base Difficulty',
      data: [4, 0],
      backgroundColor: 'rgb(255, 99, 132)',
    },
    {
      label: 'Specialisation',
      data: [0,2],
      backgroundColor: 'rgb(75, 192, 192)'
    },
    {
      label: 'Randomness',
      data: [0,2],
      backgroundColor: 'rgb(255, 205, 86)'
    },
    {
      label: 'Multiplayer',
      data: [0,2],
      backgroundColor: 'rgb(201, 203, 207)'
    }
  ]
}
var bardiff_options = {
  title: {
    display: true,
    text: "FFG vs TftC Difficulty Ratings",
    fontFamily: "'Eczar', serif",
    fontSize: 18
  },
  responsive: true,
  scales: {
    xAxes: [{
      stacked: true,
    }],
    yAxes: [{
      stacked: true,
      ticks: {
        stepSize: 1,
        min: 0,
        max: 14
      }
    }]
  }
}
  
var pieChart = new Chart($('#piechart'), {
  type: "pie", 
  data: piedata,
  options: pieoptions
});

var barChart = new Chart($('#barchart'), {
  type: "bar",
  data: bardata,
  options: baroptions
});

var diffChart = new Chart($('#diffchart'), {
  type: "bar",
  data:     bardiff_data, 
  options:  bardiff_options 
})

$.getJSON('/lotrdb/api/data/scenarios', function (data) {
  _scenarios = data;
});



$('#modaldata').on('show.bs.modal', function(e) {
  var id = parseInt($(e.relatedTarget).data('quest-id'));
  _scen = _scenarios.filter(s=>s.id==id)[0];
  $(this).find('.modal-title').html(_scen.name);
  
  updateCharts("normal");
  
});

$('#diffdown').on('click', function () {
  var newdiff = "Easy";
  if ($('#difftxt').html() == "Nightmare") {newdiff = "Normal"}
  $('#difftxt').html(newdiff);
  updateCharts(newdiff.toLowerCase());
});
$('#diffup').on('click', function () {
  var newdiff = "Nightmare";
  if ($('#difftxt').html() == "Easy") {newdiff = "Normal"}
  $('#difftxt').html(newdiff);
  updateCharts(newdiff.toLowerCase());
});
      

function updateCharts ( diff ) {
  var enemies = _scen[diff + "_enemies"];
  var locations = _scen[diff + "_locations"];
  var treacheries = _scen[diff + "_treacheries"];
  var surges = _scen[diff + "_surges"];
  var shadows = _scen[diff + "_shadows"];
  var cards = _scen[diff + "_cards"];
  var difficulty = Object.values(_scen.difficulty) // FFG, TftC Total, Base, 
  
  
  
  pieChart.data.datasets[0].data = [enemies, locations, treacheries];
  pieChart.update();
  
  barChart.data.datasets[0].data = [surges,shadows]
  barChart.data.datasets[1].data = [cards-surges, cards-shadows]
  barChart.update();

  diffChart.data.datasets[0].data = [difficulty[0],difficulty[2]];
  for (i=1; i<4; i++) {
    diffChart.data.datasets[i].data = [0,difficulty[i+2]];
  }
  diffChart.update();
}