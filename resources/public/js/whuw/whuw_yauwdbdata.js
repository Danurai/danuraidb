let _factions, _sets, _cards, _factionMembers, _waveCounts;
let url = "/whuw/api/yauwdb";

$.get(url, data => {
    _factions = data.factions;
    _factionMembers = data.factionMembers;
    _sets = data.sets;
    _cards = data.cards;
    let _waveIds = new Set( Object.keys(_cards).map( id => id[0] ) );
    _waveCounts = {};
    _waveIds.forEach( id => {
        _waveCounts[id] = Object.keys( _cards ).filter( c => c[0] == id ).length;
    });

    $('#faction').on('change', function () {
        let faction = _factions[ this.value ];
        $('#faction-members').empty();
        viewFighters( [ faction ]);
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
        viewFighters( factionArray );
        viewSetCards( set, $('#display').val()  );
    })
    $('#display').on('change', function () {
        $('#fs-toggle').hasClass('active')
            ? $('#set').trigger('change')
            : $('#faction').trigger( 'change' );
    });

    function viewFighters ( factionArray ) {
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
            let factionCardThead = $('<thead><tr><th class="d-none">ID</th><th class="text-center">Faction</th><th class="text-center">Type</th><th>Name</th><th class="text-center">Glory</th><th class="text-center">Wave</th><th class="text-center">#</th></tr></thead>');
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
                <td class="text-center">${ card.glory != null ? card.glory : ''}</td>
                <td class="text-center"><img class="icon-sm" src="/img/whuw/icons/wave-${wave.padStart( 2, '0')}-icon.png" title="Wave ${wave}"></td>
                <td class="text-center">#${card.id % 1000}/${_waveCounts[ wave ]}</td>
            </tr>`;
    }
    function cardElement( card, nameSet = true ) {
        let wave = String(Math.floor(card.id / 1000));
        let set = Object.values(_sets).filter( s => s.id == card.setId )[0];
        let faction = Object.values(_factions).filter( f => f.id == card.factionId)[0];
        let setIcons = (typeof card.duplicates != 'undefined'
            ? card.duplicates.map( id => {
                    let dupeset = Object.values(_sets).filter( s => s.id == _cards[ id ].setId )[0];
                    return `<img class="icon-sm${id != card.id ? ' icon-grey' : ''}" src="/img/whuw/icons/${dupeset.name}-icon.png" title="${dupeset.displayName}">` })
                .join(' ')
            : `<img class="icon-sm" src="/img/whuw/icons/${set.name}-icon.png" title="${set.displayName}">` );
        let gloryIcon = '<img class="icon m-1" src="/img/whuw/icons/total_glory.png">';
        let gloryIcons = (typeof card.glory != 'undefined' 
            ? `<div class="d-flex justify-content-center" style="align-items: flex-start;">${gloryIcon.repeat( card.glory )}</div>`
            : '')
        return `<div class="whuw__card d-flex flex-column m-1 border border-light rounded bg-secondary">
                <div style = "flex: 1 1 auto;" class="p-2" data-toggle="modal" data-target="#card-modal" data-cardid="${card.id}">
                    <div class="d-flex justify-content-between mb-2" style="align-items: flex-start;">
                        <img class="icon" src="/img/whuw/icons/type_${card.type.toLowerCase()}.png" title="${card.type}">
                        <img class="icon" src="/img/whuw/icons/${faction.name}-icon.png" title="${faction.displayName}">
                    </div>
                    <h5 class="text-center">${card.name}</h5>
                    <div class="text-center small">${whuw_markdown( card.rule )}</div>
                </div>
                ${gloryIcons}
                <div class="p-2">
                    <div><span>Sets: </span>${setIcons}</div>
                    <div class="small d-flex" style="align-items: flex-start;">
                        <img class="mr-1 ml-auto" style="width: 16px" src="/img/whuw/icons/wave-${wave.padStart( 2, '0')}-icon.png" title="Wave ${wave}">
                        <div>#${card.id % 1000}/${_waveCounts[ wave ]}</div>
                    </div>
                </div>
            </div>`
    }
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

function whuw_markdown( str ) {
    /*
        **...** bold
        \n newline
        [...] Attack Action
        :xxx: Symbol
    */
    return str
            .replace(/\*{2}(.*?)\*{2}/g, '<b>$1</b>')
            .replace(/\*(.*?)\*/g, '<em>$1</em>')
            .replace(/\\n/g, '<br />')
            .replace(/\:(\w+)\:/g, '<i class="icon-$1"></i>')
            .replace(/\-\((.*?)\)\-/g, '<div class="action-effect">$1</div>')
            .replace(/\s(\-|[0-9])\s\-/g, ' $1 ')
            .replace(/\[(.*?)\]/g,'<div class="h5 rounded action">$1</div><br />');
}