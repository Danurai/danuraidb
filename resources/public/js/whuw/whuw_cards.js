let _cards;
let url = "/whuw/api/yauwdb";

$.get(url, data => {
  _cards = Object.values( data.cards );

  $('#sets').on( 'change', function () {
    writetable();
  });

  $('#factions').on( 'change', function () {
    writetable();
  });

  function writetable() {
    let sets = Array.from( $('#sets').find('input:checked') ).map( ele => $(ele).data('setid') );
    let factions = $('#factions').val().map( f => parseInt( f, 10 ) );

    let cardlist = _cards.filter( c => sets.includes( c.setId )).sort( (a, b) =>  a.type > b.type ? -1 : 1 );

    if ( factions.length > 0 ) cardlist = cardlist.filter( c => factions.includes( c.factionId ));

    $('#cardlist').empty();
    cardlist.forEach( c => 
      $('#cardlist').append( `<div>${c.type} ${c.id} ${c.name}</div>`)
    );
  }

});