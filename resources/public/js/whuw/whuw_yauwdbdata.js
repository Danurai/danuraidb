import { whuw_markdown, restrictions } from './whuw_utils.js';

let _factions, _sets, _cards, _factionMembers, _cardrestrictions, _waveCounts, _formats;
let url = "/whuw/api/yauwdb";

$.get(url, data => {
    _factions = data.factions;
    _factionMembers = data.factionMembers;
    _cardrestrictions = data.cardrestrictions;
    _sets = data.sets;
    _cards = data.cards;

    $.get("/whuw/api/data", datar2 => {

        _formats = datar2.formats;

        let _waveIds = new Set( Object.keys(_cards).map( id => Math.floor(id / 1000) ) );
        _waveCounts = {};
        _waveIds.forEach( id => {
            _waveCounts[id] = Object.keys( _cards ).filter( c => Math.floor( c / 1000 ) == id ).length;
        });

        $('#faction').on('change', function () {
            let faction = _factions[ this.value ];
            $('#faction-members').empty();
            viewFighters( [ faction ], true);
            viewFactionCards( faction, $('#display').val() );
            syncSet( faction );
        });

        function syncSet( faction ) {
            let setId = Object.values( _cards ).filter( c => c.factionId == faction.id )[0].setId;
            let set = Object.values( _sets ).filter( s => s.id == setId)[0];
            $('#set').val(set.displayName);
        }

        $('#set').on('change', function () {
            let set = _sets[ this.value ];
            let factionIds = set.displayName == "Leaders" || set.displayName == "Power Unbound"
                ? new Set()
                : new Set( Object.values( _cards ).filter( c => c.setId == set.id ).map( c => c.factionId ) );
            let factionArray = Object.values( _factions ).filter( f => factionIds.has( f.id ) );
            viewFighters( factionArray, false );
            viewSetCards( set, $('#display').val()  );
        })
        $('#display').on('change', function () {
            $('#fs-toggle').hasClass('active')
                ? $('#set').trigger('change')
                : $('#faction').trigger( 'change' );
        });

        function viewFighters ( factionArray, anyset ) {
            $('#faction-members').empty();
            factionArray.forEach( f => {
                let members = _factionMembers[ f.name ];
                let factionFighters = $('<div class="whuw__faction-fighters d-flex flex-wrap justify-content-center mb-2"></div>');
                if ( typeof members != 'undefined' ) {
                    $('#faction-members').append ( factionFighters );
                    members.forEach( ( m, i ) => {
                        factionFighters.append( fighterCards( f.name, m, i + 1 ) );
                    })
                }

                if (f.name != 'universal') {
                    let rivalsFormatCheck = $('<div class="text-center"></div>')
                    let cardlist = Object.values( _cards ).filter( c => c.factionId == f.id )
                    let type = {}
                    // Objective, Upgrade, Ploy, Spell
                    type.obj = cardlist.filter( c => c.type == "Objective").length
                    type.oth = cardlist.filter( c => c.type != "Objective").length
                    type.upgrade = cardlist.filter( c => c.type == "Upgrade").length
                    type.ploy = cardlist.filter( c => c.type == "Ploy").length
                    type.spell = cardlist.filter( c => c.type == "Spell").length
                    let rivalslegal =  (type.obj == 12 && type.oth >= 20)
                        ? '<i class="fas fa-check text-success" />'
                        : '<i class="fas fa-times text-danger" />'
                    $('#faction-members').append ( rivalsFormatCheck );
                    rivalsFormatCheck.append(`<div>Objectives: ${type.obj},  Upgrade/Ploy/Spell: ${type.upgrade}/${type.ploy}/${type.spell} Rivals: ${rivalslegal}</div>`)
                }
            });
        }
        function fighterCards( factionName, m, i ) {
            let srcBase = `/img/whuw/fighters/${factionName}-${i}.webp`;
            let srcInspired = `/img/whuw/fighters/${factionName}-${i}-inspired.webp`;
            let innerHTML = `<div class="whuw__fighter-cards">
                                <div class="whuw__cards-swap-trigger"><i class="fas fa-exchange-alt fa-lg"></i></div>
                                <div class="whuw__cards-wrapper">
                                    <img src="${srcBase}" alt="${m}" class="whuw__fighter-card whuw__fighter-card-active" data-toggle = "modal" data-target = "#card-modal">
                                    <img src="${srcInspired}" alt="${m}" class="whuw__fighter-card" data-toggle = "modal" data-target = "#card-modal">
                                </div>
                            </div>`
            return innerHTML; 
        }


        function viewSetCards( set, view ) {
            viewCards( Object.values( _cards ).filter( c => c.setId == set.id ), set.displayName.match(/\w+$/)[0], view );
        }
        function viewFactionCards( faction, view ) {
            viewCards( Object.values(_cards).filter( c => c.factionId == faction.id ), "Faction", view );
        }

        function viewCards( cards, filter, view ) {
            let factionCards;
            let header = `<h5 class="text-center text-capitalize">${filter} Cards</h5>`;
            if ( view == 'List') {
                let wrapper = $('<div class="container"></div>')
                let factionCardTable = $('<table class="table table-sm text-light"></table>');
                let factionCardThead = $('<thead><tr><th class="d-none">ID</th><th class="text-center">Faction</th><th class="text-center">Type</th><th>Name</th><th>Restriction</th><th class="text-center">Glory</th><th class="text-center">Wave</th><th class="text-center">#</th></tr></thead>');
                factionCards = $('<tbody></tbody>');
                $('#card-list')
                    .empty()
                    .append( header )
                    .append( 
                        wrapper.append( 
                            factionCardTable
                                .append( factionCardThead )
                                .append( factionCards )));
            } else {
                factionCards = $('<div class="whuw__faction-cards d-flex flex-wrap justify-content-center"></div>');
                $('#card-list').empty().append( header ).append( factionCards );
            
            }
            cards.forEach( c => {
                switch ( view ) {
                    case 'List':    
                        factionCards.append( cardRow( c ) );
                        break;
                    case 'Image':   
                        factionCards.append( cardImgElement( c ) );
                        break;
                    default:        
                        factionCards.append( cardElement( c ) );
                }
            });
        }

        function cardRow( card ) {
            let wave = String(Math.floor(card.id / 1000));
            let set = Object.values(_sets).filter( s => s.id == card.setId )[0];
            let faction = Object.values(_factions).filter( f => f.id == card.factionId)[0];
            return `<tr>
                    <td class="d-none">${card.id}</td>
                    <td class="text-center"><img class="icon-sm" src="/img/whuw/icons/${faction.name}-icon.png" alt="${faction.name}" title="${faction.displayName}"></img></td>
                    <td class="text-center"><img class="icon-sm" src="/img/whuw/icons/type_${card.type.toLowerCase()}.png" alt="${card.type}" title="${card.type}"></img></td>
                    <td><span style="cursor: pointer;" data-toggle="modal" data-target="#card-modal" data-cardid="${card.id}">${card.name}</a></td>
                    <td>${ typeof _cardrestrictions[ card.id ] != 'undefined' ? _cardrestrictions[ card.id ] : ''}</td>
                    <td class="text-center">${ card.glory != null ? card.glory : ''}</td>
                    <td class="text-center"><img class="icon-sm" src="/img/whuw/icons/wave-${wave.padStart( 2, '0')}-icon.png" title="Wave ${wave}"></td>
                    <td class="text-center">#${card.id % 1000}/${_waveCounts[ wave ]}</td>
                </tr>`;
        }
        function cardElement( card ) {
            let set = Object.values(_sets).filter( s => s.id == card.setId )[0];
            let faction = Object.values(_factions).filter( f => f.id == card.factionId)[0];
            let formatRestrictions = restrictions( _formats, card );
            let wave = String(Math.floor(card.id / 1000));
            let setIcons = (typeof card.duplicates != 'undefined'
                ? card.duplicates.map( id => {
                        let dupeset = Object.values(_sets).filter( s => s.id == _cards[ id ].setId )[0];
                        return `<img class="icon-sm${id != card.id ? ' icon-grey' : ''}" src="/img/whuw/icons/${dupeset.name}-icon.png" title="${dupeset.displayName}">` })
                    .join(' ')
                : `<img class="icon-sm" src="/img/whuw/icons/${set.name}-icon.png" title="${set.displayName}">` );
            return `<div class="whuw__card d-flex flex-column m-1 border border-light rounded bg-secondary">
                    <div style = "flex: 1 1 auto;" class="p-2" data-toggle="modal" data-target="#card-modal" data-cardid="${card.id}">
                        <div class="d-flex justify-content-between mb-2" style="align-items: flex-start;">
                            <img class="icon" src="/img/whuw/icons/type_${card.type.toLowerCase()}.png" title="${card.type}">
                            <img class="icon" src="/img/whuw/icons/${faction.name}-icon.png" title="${faction.displayName}">
                        </div>
                        <h5 class="text-center">${card.name}</h5>
                        <div class="text-center small">${whuw_markdown( card.rule )}</div>
                    </div>
                    ${typeof card.glory != 'undefined' && card.glory!= null
                        ? `<div
                            class="d-flex mx-auto" 
                            style="width: 40px; height: 40px; background: url(/img/whuw/icons/total_glory.png) no-repeat; background-size: 40px;">
                            <div class="my-auto mx-auto"><b>${card.glory}</b></div>
                            </div>`
                        : ''}
                    ${ typeof _cardrestrictions[ card.id ] != 'undefined' 
                        ? `<div class="restricted">
                                <div class="restricted-header">Restricted:</div>
                                <div class="restricted-body">${_cardrestrictions[ card.id ].replace(/(leader)/i, '<i class="icon-leader"></i>').replace(/(wizard)/i, '<i class="icon-wand"></i>')}</div>
                            </div>`
                        : ''}
                    <div class="p-2">
                        <div><span>Sets: </span>${setIcons}</div>
                        <div class="small d-flex" style="align-items: flex-start;">
                            <img class="mr-1 ml-auto" style="width: 16px" src="/img/whuw/icons/wave-${wave.padStart( 2, '0')}-icon.png" title="Wave ${wave}">
                            <div>#${card.id % 1000}/${_waveCounts[ wave ]}</div>
                        </div>
                    </div>
                </div>`
        }
        //<div>${JSON.stringify(formatRestrictions)}</div>
                    
        function warhammerUnderworldsCardURL( card ) {
            let setPrefix = [ '-', 'S', 'L', 'NV', 'NVPU', 'COD', 'BG', 'BGGIFTPACK', 'ARENAMORTIS', 'DIRECHASM' ];
            let cycle = Math.round( card.id / 1000 );
            let setCardId = String( card.id % 1000 );
            let cardImg =  setPrefix[ cycle ] + ( cycle == 1 ? setCardId.padStart( 3, '0' ) : cycle == 2 ? setCardId.padStart( 2, '0' ) : setCardId );
            return `https://images.warhammerunderworlds.com/en/${cardImg}.png`;
        }
        function cardImgElement( card ) {
            let src = warhammerUnderworldsCardURL( card );
            return `<img class = "whuw__card m-2" src = "${src}" data-toggle = "modal" data-target = "#card-modal" alt = "${card.id + ': ' + card.name}" title="${card.name}">`
        }

        $('#faction-members')
            .on('click', '.whuw__cards-swap-trigger', function () {
                $('#faction-members').find('.whuw__fighter-card').toggleClass('whuw__fighter-card-active')
            });
        
        $('#card-modal').on('show.bs.modal', function (evt) {
            let src = evt.relatedTarget.src;
            if (typeof src == 'undefined') {
                $('#card-modal').find('.modal-body').html(
                    cardElement( _cards[ $(evt.relatedTarget).data('cardid') ] )
                );    
            } else {
                $('#card-modal').find('.modal-body').html(
                    `<picture><img class="img-fluid" src="${src}"></picture>`   //<source srcset = "${src}_xs.webp">
                );
            }
        });

        $('#fs-toggle').on('click', function() {
            $('#factionset select').toggleClass('d-none');
            if ( $(this).hasClass('active') ) {
                $('#fs-label').text("Faction");
                $('#faction').trigger('change');
            } else {
                $('#fs-label').text("Set");
                $('#set').trigger('change');
            };
        })

        $('#set')[0].value = $('#set option').last().val();
        $('#faction')[0].value = $('#faction option').last().val();
        $('#faction').trigger('change');
    });

});