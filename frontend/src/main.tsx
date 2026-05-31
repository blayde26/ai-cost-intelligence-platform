import React from 'react';
import ReactDOM from 'react-dom/client';
import { CssBaseline, ThemeProvider, createTheme } from '@mui/material';
import App from './App';
import './styles.css';

const theme = createTheme({
  palette: {
    mode: 'light',
    primary: { main: '#26547c' },
    secondary: { main: '#2a9d8f' },
    warning: { main: '#f4a261' },
    error: { main: '#b23a48' },
    background: {
      default: '#f6f7f9',
      paper: '#ffffff'
    }
  },
  shape: {
    borderRadius: 6
  },
  typography: {
    fontFamily: '"Inter", "Segoe UI", Arial, sans-serif',
    h1: { fontSize: '1.65rem', fontWeight: 700, letterSpacing: 0 },
    h2: { fontSize: '1.2rem', fontWeight: 700, letterSpacing: 0 },
    h3: { fontSize: '1rem', fontWeight: 700, letterSpacing: 0 },
    button: { textTransform: 'none', letterSpacing: 0 }
  },
  components: {
    MuiButtonBase: {
      defaultProps: {
        disableRipple: true
      }
    },
    MuiTableCell: {
      styleOverrides: {
        root: {
          borderBottom: '1px solid #e6e8ec'
        },
        head: {
          fontWeight: 700,
          color: '#4b5563'
        }
      }
    }
  }
});

ReactDOM.createRoot(document.getElementById('root') as HTMLElement).render(
  <React.StrictMode>
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <App />
    </ThemeProvider>
  </React.StrictMode>
);
