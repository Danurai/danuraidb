const e = React.createElement;


class DataTable extends React.Component {
    constructor(props) {
        super(props);
    }
    
    componentDidUpdate() {
        let faction_map = {};
        let set_map = {};
        this.props.factions.forEach(faction => {
            faction_map[faction.id] = faction;
        });
        this.props.sets.forEach(set => {
            set_map[set.id] = set;
        });
        let cards = this.props.data.map( card => ({
            id: card.id, 
            name: card.name,
            faction: {
                id: card.factionId, 
                display: `<img class="icon-sm" src="/img/whuw/icons/${faction_map[card.factionId].name}-icon.png" title="${faction_map[card.factionId].displayName}" />`,
                name: faction_map[card.factionId].name
            },
            set: {
                id: card.setId,
                display: `<img class="icon-sm" src="/img/whuw/icons/${set_map[card.setId].name}-icon.png" title="${set_map[card.setId].displayName}" />`,
                name: set_map[card.setId].name
            }
        }));
        $(this.refs.main).DataTable({
            data: cards,
            columns: [
                { title: 'Id', data: 'id' },
                { title: 'name', data: 'name' },
                { title: 'F', data: 'faction', render: { display: 'display', _: 'id'}},
                { title: 'S', data: 'set', render: { display: 'display', _: 'id'}}
            ]
        });
    }


    render() {
        return e('table',{className: 'table table-sm table-banded', ref: 'main'},null)
    }
}

class Main extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            cards: [],
            sets: [],
            factions: [],
            error: null,
            loaded: false
        }
    }

    componentDidMount() {
        fetch('/whuw/api/yauwdb')
            .then(resp => resp.json())
            .then(data => {
                this.setState({
                    loaded: true,
                    cards: Object.values(data.cards),
                    sets: Object.values(data.sets),
                    factions: Object.values(data.factions)
                });
            })
            .catch(error => { 
                console.log(error);
                this.setState({
                    loaded: true,
                    error
                });
            });
        }
    render() {
        return e(
            'div',
            {className: "container py-3"},
            e('div', {className: 'row'}, 
                [
                    e('div', {className: 'col-sm-6'}, "Col 1"),
                    e('div', {className: 'col-sm-6'}, 
                        e(DataTable, {data: this.state.cards, factions: this.state.factions, sets: this.state.sets, error: this.state.error, loaded: this.state.loaded})
                    ),
                ]
            )
        )
    }
}

//import ReactDom from 'react-dom.production.min';
ReactDOM.render(
    e(Main, null, null),
    $('#whuwmortisdbapp')[0]
)