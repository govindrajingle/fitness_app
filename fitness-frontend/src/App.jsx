import {BrowserRouter as Router, Route, Routes, useNavigate} from "react-router";
import Button from "@mui/material/Button";
import {useContext, useEffect, useState} from "react";
import {AuthContext} from "react-oauth2-code-pkce";
import {useDispatch} from "react-redux";
import {logout, setCredentials} from "./store/authSlice";
import {Box} from "@mui/material";
import ActivityForm from "./components/ActivityForm";
import ActivityList from "./components/ActivityList";
import ActivityDetail from "./components/ActivityDetail"; // if using MUI

const ActivityPage = () => {
    return (<Box sx={{flexGrow: 1}}>
        <ActivityForm onActivityAdded={() => window.location.reload()}/>
        <ActivityList/>
    </Box>);
};

// A small wrapper so we can use navigate inside
const GoToActivitiesButton = () => {
    const navigate = useNavigate();
    return (<Button
        variant="contained"
        onClick={() => navigate("/activities")}
    >
        Go to Activities
    </Button>);
};

function App() {
    const {token, tokenData, logIn} = useContext(AuthContext);
    const dispatch = useDispatch();
    const [authReady, setAuthReady] = useState(false);

    useEffect(() => {
        if (token) {
            dispatch(setCredentials({token, user: tokenData}));
            setAuthReady(true);
        }
    }, [token, tokenData, dispatch]);

    return (<Router>
        {!token ? (<Button variant="contained" onClick={() => logIn()}>
            LOGIN
        </Button>) : (<div>
                    <pre>
                        <Box component="section" sx={{p: 2, border: "1px dashed grey"}}>
                            <Button variant="contained" onClick={logout}>
                                LOGOUT
                            </Button>
                            <Routes>
                                <Route path="/activities" element={<ActivityPage/>}/>
                                <Route path="/activities/:id" element={<ActivityDetail/>}/>
                            </Routes>
                        </Box>
                    </pre>
            {/* Second button goes here */}
            <GoToActivitiesButton/>
        </div>)}
    </Router>);
}

export default App;
