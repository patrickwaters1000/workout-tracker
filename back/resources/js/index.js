const e = React.createElement;

const styles = {
    table: {
        fontFamily: "arial",
        borderCollapse: "collapse"
    },
    childrenArrangedHorizontally: {
        display: "flex",
        flexDirection: "row"
    },
    childrenArrangedVertically: {
        display: "flex",
        flexDirection: "column"
    }
};

const columnHeaders = {
    distance: "Distance (mi)",
    pace: "Pace (min / mi)",
    heartRate: "Heart rate (/ min)",
    date: "Date",
    intervals: "Intervals"
};

const inputIntervalColumns = ["distance", "pace", "heartRate"];
const inputRunColumns = ["date", "distance", "pace", "heartRate"];
const runsTableColumns = ["date", "distance", "pace", "heartRate", "intervals"];

const formatSeconds = seconds => {
    if (seconds < 10) {
        return `0${seconds}`;
    }
    return `${seconds}`;
};

const formatPace = pace => {
    let minutes = Math.floor(pace).toFixed(0);
    let seconds = Math.floor((pace % 1) * 60).toFixed(0);
    return `${minutes}:${formatSeconds(seconds)}`;
};

const formatIntervals = intervals => intervals.map(interval => {
    let {distance, pace} = interval;
    return`${distance.toFixed(2)} @ ${formatPace(pace)}`;
}).join("\n");
//div = e("div", {style: styles.childrenArrangedVertically}, strs)


const columnFormatters = {
    date: x => x,
    distance: x => x.toFixed(2),
    pace: formatPace,
    heartRate: x => (x ? x.toFixed(0) : ""),
    intervals: formatIntervals
};

const Table = (props) => {
    let { columns,
          body,
          width } = props;
    let ths = columns.map(c => e("th", {}, columnHeaders[c]));
    let trs = body.map(row => {
        let tds = columns.map(c => {
            let value = row[c];
            let formatFn = columnFormatters[c];
            let text = formatFn(value);
            return e("td", {style: {whiteSpace: "pre"}}, text);
        });
        return e("tr", {}, tds);
    });
    let style = { ...styles.table, width: width };
    return e(
        "table",
        { style: style },
        e("thead", {}, e("tr", {}, ...ths,)),
        e("tbody", {}, ...trs)
    );
};

const InputTable = (props) => {
    let { columns,
          body,
          width,
          update } = props;
    let style = { ...styles.table, width: width };
    let ths = columns.map(c => e("th", {}, columnHeaders[c]));
    let trs = body.map((row, rowIdx) => {
        let tds = columns.map(c => {
            let onChange = (event) => {
                update(rowIdx, c, event.target.value);
            };
            let input = e("input", { onChange: onChange});
            return e("td", {}, input);
        });
        return e(
            "tr", {}, tds
        );
    });
    return e(
        "table",
        { style: style },
        e("thead", {}, e("tr", {}, ...ths,)),
        e("tbody", {}, ...trs)
    );
};

const InputRunDiv = (props) => {
    let { inputRunTableRow,
          updateInputRun,
          inputIntervalsTableBody,
          updateInputIntervals } = props;
    let style = { display: "flex", flexDirection: "column" };
    let runHeader = e("h2", {}, "Add run");
    let inputRunTable = InputTable({
        columns: inputRunColumns,
        body: [inputRunTableRow],
        update: updateInputRun,
        width: "50%"
    });
    let intervalsHeader = e("h3", {}, "Intervals");
    let inputIntervalsTable = InputTable({
        columns: inputIntervalColumns,
        body: inputIntervalsTableBody,
        update: updateInputIntervals,
        width: "50%"
    });
    let addIntervalButton = e("button", { style: {width: "100px"}, onClick: addInterval }, "Add interval")
    let submitButton = e("button", { style: {width: "100px"}, onClick: submitRun }, "Submit");
    return e(
        "div",
        { style: style },
        runHeader,
        inputRunTable,
        intervalsHeader,
        inputIntervalsTable,
        addIntervalButton,
        submitButton,
    );
};


const RunsTable = (props) => {
    let { columns,
          body,
          page,
          numRows } = props;
    let header = e("h2", {}, "Runs");
    let prevPageButton = e("button", { onClick: prevRunsTablePage, style: { width: "50px"}}, "<");
    let nextPageButton = e("button", { onClick: nextRunsTablePage, style: { width: "50px"}}, ">");
    let pageButtonsDiv = e(
        "div",
        {style: styles.childrenArrangedHorizontally},
        prevPageButton,
        nextPageButton
    );
    let visibleRows = body.reverse().slice(
        page * numRows,
        (page + 1) * numRows
    );
    let table = Table({
        columns: columns,
        body: visibleRows
    });
    return e(
        "div",
        {},
        header,
        pageButtonsDiv,
        table
    );
};

const Page = (props) => {
    let { runsTableBody,
          inputRunTableRow,
          inputIntervalsTableBody,
          runsTablePage,
          numRunsTableRows,
          updateInputRun,
          updateInputIntervals } = props;
    let runsTable = RunsTable({
        columns: runsTableColumns,
        body: runsTableBody,
        numRows: numRunsTableRows,
        page: runsTablePage,
    });
    let children = [runsTable];
    if (!inputRunTableRow) {
        let button = e(
            "button",
            { onClick: createInputRun,
              style: {...styles.button, width: "50px" }},
            "+"
        );
        children.push(button);
    } else {
        let div = InputRunDiv({
            inputRunTableRow: inputRunTableRow,
            updateInputRun: updateInputRun,
            inputIntervalsTableBody: inputIntervalsTableBody,
            updateInputIntervals: updateInputIntervals
        });
        children.push(div);
    }
    return e("div", {}, children);
}

const syncState = () => {
    const props = {
        ...JSON.parse(JSON.stringify(state)),
        updateInputRun: updateInputRun,
        updateInputIntervals: updateInputIntervals
    };
    const newPage = e(Page, props);
    root.render(newPage);
};

const updateRunsTable = (page) => {
    fetch(
        "runs"
    ).then(
        resp => resp.json()
    ).then(json => {
        console.log(json);
        state.runsTableBody = json.runs;
        syncState();
    });
};

const updateInputRun = (_, column, value) => {
    state.inputRunTableRow[column] = value;
};

const updateInputIntervals = (rowIdx, column, value) => {
    state.inputIntervalsTableBody[rowIdx][column] = value;
};

const nextRunsTablePage = () => {
    let maxPage = Math.floor(state.runsTableBody.length / state.numRunsTableRows);
    if (state.runsTablePage < maxPage) {
        state.runsTablePage += 1;
    }
    syncState();
};

const prevRunsTablePage = () => {
    if (state.runsTablePage > 0) {
        state.runsTablePage -= 1;
    }
    syncState();
};

const submitRun = () => {
    console.log(JSON.stringify(state));
    let run = state.inputRunTableRow;
    run.intervals = state.inputIntervalsTableBody;
    fetch("add-run", {
        method: 'POST',
        headers: { 'Content-Type': 'application/json;charset=utf-8' },
        body: JSON.stringify(run)
    }).then(
        resp => resp.json()
    ).then(json => {
        console.log(json);
        state.runsTableBody = json.runs;
        state.inputRunTableRow = {date: null, distance: null, pace: null, heartRate: null};
        state.inputIntervalsTableBody = [];
        syncState();
    });
};

const addInterval = () => {
    row = {distance: null, pace: null, heartRate: null};
    state.inputIntervalsTableBody.push(row);
    syncState();
};

const createInputRun = () => {
    state.inputRunTableRow = {date: null, distance: null, pace: null, heartRate: null};
    syncState();
};

let state = {
    numRunsTableRows: 10,
    runsTablePage: 0,
    runsTableBody: [],
    inputRunTableRow: null,
    inputIntervalsTableBody: []
};

const page = e(Page, state);
const mainDiv = document.getElementById("main");
const root = ReactDOM.createRoot(mainDiv);
root.render(page);
updateRunsTable();

const generateDistancePlot = (date, distance) => {
    const series = [{
        name: "Weekly distance",
        x: date,
        y: distance,
    }];
    console.log(series);
    const layout = {
        title: "Weekly distance",
        xaxis: {title: "Date"},
        yaxis: {title: "Distance (mi)"}

    };
    div = document.getElementById("plot");
    Plotly.newPlot(div, series, layout);
};

const fetchDataAndGenerateDistancePlot = () => {
    fetch(
        "running-distance-plot"
    ).then(
        resp => resp.json()
    ).then(json => {
        console.log(json);
        let {date, distance} = json;
        generateDistancePlot(date, distance);
    });
}
fetchDataAndGenerateDistancePlot();
