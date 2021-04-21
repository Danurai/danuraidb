import { whuw_markdown } from './whuw_utils.js';

let _cards, _sets, _factions;
let _cardrestrictions;

$.get("/whuw/api/yauwdb", data => {
    let _setIdMap = [];
    Object.values( data.sets ).forEach( s => {
        _setIdMap[ s.id ] = s.displayName; 
    });
    _cards = Object.values( data.cards ).map( c => {
        c.setName = _setIdMap[ c.setId ];
        return c;
    });
    _factions = Object.values( data.factions );
    _sets = Object.values( data.sets );
    _cardrestrictions = data.cardrestrictions;

    let ownedSets = getOwnedSets();
    ownedSets.forEach( s => $('#sets').find(`input[data-setid=${s}]`).attr('checked',true));
    updateCollectionCount();
    updateOwnedCards();


    $('.typeahead').typeahead({
        hint: true, highlight: true, minLength: 2
      },{
        name: 'cardnames',
        source: 
          function findMatches (q, cb) {
            let re = new RegExp( q, 'i' );
            cb(_cards.map( c => `${c.name} (${c.setName})` ).filter( c => c.match( re ) ));}
      });
    
    $('#searchcards').on('typeahead:select typeahead:autocomplete input',function () {
      let val = $(this).val();
      let cardmatch = _cards.filter( c => `${c.name} (${c.setName})` == val );
      $('#cardinfo').empty();
      if ( cardmatch.length > 0 ) {
        let card = cardmatch[0];
        let faction = Object.values(_factions).filter( f => f.id == card.factionId )[0];
        let gloryIcon = '<img class="icon m-1" src="/img/whuw/icons/total_glory.png">';
        
        $('#cardinfo').html(
            `<div class="w-75 mx-auto my-3">
                <div class="d-flex justify-content-around">
                    <span><span class="mr-2">Owned</span>${tficon( isInCollection ( card.id ) )}</span>
                </div>
                <div class="d-flex justify-content-between mb-2">
                    <img style="align-items: flex-start; width: 40px;" src="/img/whuw/icons/type_${card.type.toLowerCase()}.png"></img>
                    <h4 class="my-auto">${card.name}</h4>
                    <img style="align-items: flex-start; width: 40px;" src="/img/whuw/icons/${faction.name}-icon.png"></img>
                </div>
                <div class="text-center">${whuw_markdown( card.rule )}</div>
            </div>`
        );
      }
    });
    function tficon( bool ) {
        return bool ? '<i class="fa fa-check-circle text-success"></i>' : '<i class="fa fa-times-circle text-danger"></i>'
    };
    $('#addcards').on('typeahead:select typeahead:autocomplete',function () {
        let val = $(this).val();
        let cardmatch = _cards.filter( c => `${c.name} (${c.setName})` == val );
        $('#cardinfo').empty();
        if ( cardmatch.length > 0 ) {
          let card = cardmatch[0];
          let ownedCards = getOwnedCards();
          ownedCards.push( card.id );
          window.localStorage.setItem( 'whuwownedcards', JSON.stringify( ownedCards ) );
          updateOwnedCards();
        }
      });

    function isInCollection( cardid ) {
        let ownedSets = getOwnedSets();
        let ownedCards = _cards.filter( c => ownedSets.includes( c.setId ) ).map( c => c.id ).concat( getOwnedCards() );
        return ownedCards.includes( cardid );
    }

    $('#sets').on('change', function () {
        window.localStorage.setItem( 'whuwsets2', JSON.stringify( Array.from( $('#sets').find('input:checked') ).map( s => $(s).data('setid') ) ) );
        updateCollectionCount();
    });

    $('#extracardlist').on('click', '.removeextracard', function () {
        let ownedCards = getOwnedCards();
        let cardid = $(this).data('cardid');
        window.localStorage.setItem( 'whuwownedcards', JSON.stringify( ownedCards.filter( c => c != cardid ) ) );
        updateOwnedCards();
        updateCollectionCount();
    })


    function updateCollectionCount() {
        let ownedSets = getOwnedSets();
        let ownedCards = _cards.filter( c => ownedSets.includes( c.setId ) ).concat( getOwnedCards() );
        $('#collection').html(`Collection <small>(${ownedCards.length}/${_cards.length})</small>`);
    }
    function updateOwnedCards() {
        $('#extracardlist').empty();
        let ownedCards = getOwnedCards();
        _cards
            .filter( c => ownedCards.includes( c.id ) )
            .forEach( c => $('#extracardlist')
                .append( `<div><i class="fa fa-times fa-sm removeextracard text-danger mr-2" data-cardid="${c.id}"></i><span>${c.name} (${c.setName})</span></div>` ));
    }

    function getOwnedSets() {
        let lsSets = window.localStorage.getItem('whuwsets2');
        return lsSets == null || lsSets == "" ? [] : JSON.parse( lsSets );
    }
    function getOwnedCards() {
        let lscards = window.localStorage.getItem('whuwownedcards');
        return lscards == null || lscards == "" ? [] : JSON.parse( lscards );
    }
});