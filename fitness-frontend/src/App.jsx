import {
  BrowserRouter as Router,
  Navigate,
  Route,
  Routes,
  useLocation,
} from "react-router";

import Button from "@mui/material/Button"; // if using MUI

function App() {
  return (
    <>
      <Router>
        <Button variant="contained">Log in</Button>
      </Router>
    </>
  );
}

export default App;
