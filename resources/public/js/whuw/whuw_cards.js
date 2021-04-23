import { restrictions } from './whuw_utils.js';

let _cards, _formats, _sets, _factions;
let url = "/whuw/api/yauwdb";
let url2 = "/whuw/api/data";

$.get( url, data => {
  _cards = Object.values( data.cards );
  _sets = Object.values( data.sets );
  _factions = Object.values( data.factions );

  $.get( url2, data2 => {
    _formats = data2.formats;

    _cards = _cards.map( c => {
      c.formats = restrictions( _formats, c );
      return c;
    });

    $('#sets').on( 'change', function () {
      writetable();
    });

    $('#factions').on( 'change', function () {
      writetable();
    });

    $('#tablefilter').on('input', function() {
      writetable();
    })

    function formatele ( formats, f ) {
      switch ( formats[ f ] ) {
        case "Y+": 
          return `<b class="text-primary" title="${f}: Allowed (Newer Card)">Y</b>`;
          break;
        case "N": 
          return `<b class="text-danger" title="${f}: Not Allowed">N</b>`;
          break;
        case "F": 
          return `<b class="text-warning" title="${f}: Forsaken">F</b>`;
          break;
        case "R": 
          return `<b class="text-info" title="${f}: Restricted">R</b>`;
          break;
        default:
          return `<b class="text-success" title="${f}: Allowed">Y</b>`;
      }
    }

    function writetable() {
      let sets = Array.from( $('#sets').find('input:checked') ).map( ele => $(ele).data('setid') );
      let factions = $('#factions').val().map( f => parseInt( f, 10 ) );
      
      let namefilter = $('#tablefilter').val();
      
      let cardlist = _cards.filter( c => sets.includes( c.setId )).sort( (a, b) =>  a.type > b.type ? -1 : 1 );

      if ( factions.length > 0 ) cardlist = cardlist.filter( c => factions.includes( c.factionId ));
      if ( namefilter != "" ) cardlist = cardlist.filter( c => { let re = new RegExp( namefilter, 'i' ); return c.name.match( re ) != null })
      let tbody = $('#cardlist tbody');
      tbody.empty();
      cardlist.forEach( c => {
        let f = _factions.filter( f => f.id == c.factionId )[0];
        tbody.append( `<tr>
          <td class="text-center d-none">${c.id}</td>
          <td class="text-center"><img class="icon-sm" src="/img/whuw/icons/type_${c.type.toLowerCase()}.png" title="${c.type}"></img></td>
          <td class="text-center"><img class="icon-sm" src="/img/whuw/icons/${f.displayName.toLowerCase()}-icon.png" title="${f.displayName}"></img></td>
          <td title="id: ${c.id}">${c.name}</td>
          ${ Object.keys(c.formats)
              .map( f => `<td class="text-center">${ formatele( c.formats, f )}</td>` )
              .join( '' )}
          </tr>`)
      });
    }

    $.each( $('#sets').find("[data-setid]") , function () {
      if ( $(this).data('setid') < 8 ) $(this).attr('checked', true);
    });
    $('#factions').val("1");
    writetable();
  });
});