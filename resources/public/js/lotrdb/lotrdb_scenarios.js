var _scenarios;
var normalpie = document.getElementById("normalpiechart");
var normalbar = document.getElementById("normalbarchart");

$.getJSON('/lotrdb/api/data/scenarios', function (data) {
  _scenarios = data;
});



$('#modaldata').on('show.bs.modal', function(e) {
  var id = parseInt($(e.relatedTarget).data('quest-id'));
  var s = _scenarios.filter(s=>s.id==id)[0];
  $(this).find('.modal-title').html(s.name);
  
  
  var normalPie = new Chart(normalpie, {
    type: "pie", 
    data: {
      labels: ['Enemy', 'Location', 'Treachery'], //objectives
      datasets: [{
        data: [s.normal_enemies, s.normal_locations, s.normal_treacheries],
        backgroundColor: ['rgba(128,0,0,0.2)','rgba(0,128,0,0.2)','rgba(0,0,128,0.2)']
      }]   
    } //, options: options
  });
  
  var normalBar  = new Chart(normalbar, {
    type: "bar",
    data: {
      labels: ['Surge','Shadow'],
      datasets: [{
        label: 'In Deck',
        data: [s.normal_surges,s.normal_shadows],
        backgroundColor: ['rgba(128,0,0,0.2)','rgba(0,128,0,0.2)'],
        borderColor: ['rgba(128,128,128,0.6)','rgba(128,128,128,0.6)'],
        borderWidth: 1
      },{
        label: 'Other cards',
        data: [s.normal_cards-s.normal_surges, s.normal_cards-s.normal_shadows],
        backgroundColor: ['rgba(0,0,0,0)','rgba(0,0,0,0)'],
        borderColor: ['rgba(128,128,128,0.6)','rgba(128,128,128,0.6)'],
        borderWidth: 1
      }]
    },
    options: {
      scales: {
        xAxes: [{
          stacked: true
        }],
        yAxes: [{
          stacked: true
        }]
      }
    }
  }) ;
});