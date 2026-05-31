import {
  Alert,
  AppBar,
  Box,
  Button,
  Chip,
  CircularProgress,
  Container,
  Divider,
  IconButton,
  LinearProgress,
  MenuItem,
  Paper,
  Stack,
  Tab,
  Tabs,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Toolbar,
  Tooltip,
  Typography
} from '@mui/material';
import AccountTreeIcon from '@mui/icons-material/AccountTree';
import GroupsIcon from '@mui/icons-material/Groups';
import HealthAndSafetyIcon from '@mui/icons-material/HealthAndSafety';
import ListAltIcon from '@mui/icons-material/ListAlt';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import PieChartIcon from '@mui/icons-material/PieChart';
import RefreshIcon from '@mui/icons-material/Refresh';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import { useEffect, useMemo, useState } from 'react';
import {
  AttributionCorrectionRequest,
  AttributionCoverage,
  PotentialWaste,
  SpendAllocation,
  SpendByEpic,
  SpendByStory,
  SpendByTeam,
  SpendOverview,
  UsageEvent,
  api
} from './api';
import { compactNumber, integer, money, percent, timestamp } from './format';

type View = 'overview' | 'teams' | 'epics' | 'attribution' | 'waste' | 'usage';

type DashboardData = {
  overview: SpendOverview;
  coverage: AttributionCoverage;
  allocation: SpendAllocation;
  waste: PotentialWaste;
  stories: SpendByStory[];
  epics: SpendByEpic[];
  teams: SpendByTeam[];
  events: UsageEvent[];
};

const viewConfig: Array<{ value: View; label: string; icon: JSX.Element }> = [
  { value: 'overview', label: 'Overview', icon: <PieChartIcon fontSize="small" /> },
  { value: 'teams', label: 'Team Analysis', icon: <GroupsIcon fontSize="small" /> },
  { value: 'epics', label: 'Epic Analysis', icon: <AccountTreeIcon fontSize="small" /> },
  { value: 'attribution', label: 'Attribution Health', icon: <HealthAndSafetyIcon fontSize="small" /> },
  { value: 'waste', label: 'Potential Waste', icon: <WarningAmberIcon fontSize="small" /> },
  { value: 'usage', label: 'Usage Explorer', icon: <ListAltIcon fontSize="small" /> }
];

function routeFromHash(): { view: View; eventId?: string } {
  const hash = window.location.hash.replace(/^#\/?/, '');
  const [view, id] = hash.split('/');
  if (viewConfig.some((item) => item.value === view)) {
    return { view: view as View, eventId: id };
  }
  if (view === 'request' && id) {
    return { view: 'usage', eventId: id };
  }
  return { view: 'overview' };
}

export default function App() {
  const [route, setRoute] = useState(routeFromHash());
  const [data, setData] = useState<DashboardData | null>(null);
  const [selectedEvent, setSelectedEvent] = useState<UsageEvent | null>(null);
  const [loading, setLoading] = useState(true);
  const [detailLoading, setDetailLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const onHashChange = () => setRoute(routeFromHash());
    window.addEventListener('hashchange', onHashChange);
    return () => window.removeEventListener('hashchange', onHashChange);
  }, []);

  useEffect(() => {
    void loadDashboard();
  }, []);

  useEffect(() => {
    if (!route.eventId) {
      setSelectedEvent(null);
      return;
    }
    const cached = data?.events.find((event) => event.id === route.eventId);
    if (cached) {
      setSelectedEvent(cached);
      return;
    }
    setDetailLoading(true);
    api
      .usageEvent(route.eventId)
      .then(setSelectedEvent)
      .catch((exception: Error) => setError(`Unable to load request ${route.eventId}: ${exception.message}`))
      .finally(() => setDetailLoading(false));
  }, [data?.events, route.eventId]);

  async function loadDashboard() {
    setLoading(true);
    setError(null);
    try {
      const [overview, coverage, allocation, waste, stories, epics, teams, events] = await Promise.all([
        api.overview(),
        api.coverage(),
        api.allocation(),
        api.potentialWaste(),
        api.spendByStory(),
        api.spendByEpic(),
        api.spendByTeam(),
        api.usageEvents()
      ]);
      setData({ overview, coverage, allocation, waste, stories, epics, teams, events });
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Unable to load dashboard data');
    } finally {
      setLoading(false);
    }
  }

  function navigate(view: View) {
    window.location.hash = `/${view}`;
  }

  async function handleAttributionCorrected(event: UsageEvent) {
    setSelectedEvent(event);
    await loadDashboard();
  }

  return (
    <Box className="app-shell">
      <AppBar position="sticky" color="inherit" elevation={0} className="topbar">
        <Toolbar className="toolbar">
          <Stack spacing={0.25}>
            <Typography variant="h1">AI Cost Intelligence</Typography>
            <Typography variant="body2" color="text.secondary">
              Where AI investment is going across teams, work, and attribution quality
            </Typography>
          </Stack>
          <Tooltip title="Refresh data">
            <span>
              <IconButton aria-label="Refresh data" onClick={() => void loadDashboard()} disabled={loading}>
                <RefreshIcon />
              </IconButton>
            </span>
          </Tooltip>
        </Toolbar>
        <Tabs
          value={route.view}
          onChange={(_, value) => navigate(value)}
          variant="scrollable"
          scrollButtons="auto"
          className="tabs"
        >
          {viewConfig.map((view) => (
            <Tab key={view.value} value={view.value} icon={view.icon} iconPosition="start" label={view.label} />
          ))}
        </Tabs>
      </AppBar>

      <Container maxWidth="xl" className="content">
        {error && (
          <Alert severity="error" className="section">
            {error}
          </Alert>
        )}
        {loading && (
          <Box className="loading">
            <CircularProgress size={28} />
            <Typography color="text.secondary">Loading ACIP dashboard data</Typography>
          </Box>
        )}
        {!loading && data && (
          <>
            {route.view === 'overview' && <OverviewView data={data} />}
            {route.view === 'teams' && <TeamAnalysisView teams={data.teams} />}
            {route.view === 'epics' && <EpicAnalysisView epics={data.epics} stories={data.stories} />}
            {route.view === 'attribution' && <AttributionHealthView coverage={data.coverage} events={data.events} />}
            {route.view === 'waste' && <PotentialWasteView waste={data.waste} />}
            {route.view === 'usage' && (
              <UsageExplorerView
                events={data.events}
                selectedEvent={selectedEvent}
                detailLoading={detailLoading}
                stories={data.stories}
                onCorrected={(event) => void handleAttributionCorrected(event)}
              />
            )}
          </>
        )}
      </Container>
    </Box>
  );
}

function OverviewView({ data }: { data: DashboardData }) {
  const topTeam = data.teams[0];
  const topEpic = data.epics[0];
  return (
    <Stack spacing={2.5}>
      <Paper className="panel hero-panel">
        <Stack spacing={2}>
          <Stack spacing={0.5}>
            <Typography variant="h2">AI Spend Allocation</Typography>
            <Typography color="text.secondary">
              Spend split by business intent, attribution gaps, and potential waste signals.
            </Typography>
          </Stack>
          <AllocationBars allocation={data.allocation} />
        </Stack>
      </Paper>
      <Box className="metric-grid">
        <MetricCard label="Total AI spend" value={money(data.overview.totalSpend)} />
        <MetricCard label="Attribution coverage" value={percent(data.coverage.coveragePercent)} detail={`${integer(data.coverage.validEventCount)} valid / ${integer(data.coverage.eventCount)} events`} />
        <MetricCard label="Potential waste" value={money(wasteTotal(data.waste))} detail="Observed, not enforced" />
        <MetricCard label="Top spending team" value={topTeam?.teamKey ?? 'None'} detail={topTeam ? money(topTeam.totalCost) : undefined} />
      </Box>
      <Box className="two-column">
        <Paper className="panel">
          <Typography variant="h2">Top Epics</Typography>
          <MiniSpendList rows={data.epics.slice(0, 5).map((epic) => ({
            key: epic.epicKey,
            label: epic.epicName ?? 'No epic found',
            cost: epic.totalCost,
            tokens: epic.totalTokens
          }))} />
        </Paper>
        <Paper className="panel">
          <Typography variant="h2">Current Focus</Typography>
          <DetailRow label="Top epic" value={topEpic ? `${topEpic.epicKey} (${money(topEpic.totalCost)})` : 'None'} />
          <DetailRow label="Total requests" value={integer(data.overview.totalRequests)} />
          <DetailRow label="Total tokens" value={integer(data.overview.totalTokens)} />
          <DetailRow label="Unattributed spend" value={money(data.coverage.unattributedCost)} />
        </Paper>
      </Box>
    </Stack>
  );
}

function AllocationBars({ allocation }: { allocation: SpendAllocation }) {
  if (allocation.buckets.length === 0) {
    return <EmptyState />;
  }
  return (
    <Stack spacing={1.25}>
      {allocation.buckets.map((bucket) => (
        <Box key={bucket.category} className="allocation-row">
          <Stack direction="row" justifyContent="space-between" gap={2}>
            <Typography fontWeight={750}>{bucket.category}</Typography>
            <Typography fontWeight={750}>{percent(bucket.percent)} / {money(bucket.totalCost)}</Typography>
          </Stack>
          <Box className="bar-track">
            <Box className="bar-fill" sx={{ width: `${Math.max(bucket.percent, bucket.totalCost > 0 ? 4 : 0)}%` }} />
          </Box>
        </Box>
      ))}
    </Stack>
  );
}

function TeamAnalysisView({ teams }: { teams: SpendByTeam[] }) {
  return (
    <ReportPanel title="Cost by Team">
      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Team</TableCell>
              <TableCell align="right">Requests</TableCell>
              <TableCell align="right">Tokens</TableCell>
              <TableCell align="right">Epics</TableCell>
              <TableCell align="right">Stories</TableCell>
              <TableCell align="right">Cost</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {teams.map((team) => (
              <TableRow key={team.teamKey}>
                <TableCell><Typography fontWeight={700}>{team.teamKey}</Typography></TableCell>
                <TableCell align="right">{integer(team.requestCount)}</TableCell>
                <TableCell align="right">{integer(team.totalTokens)}</TableCell>
                <TableCell align="right">{integer(team.epicCount)}</TableCell>
                <TableCell align="right">{integer(team.storyCount)}</TableCell>
                <TableCell align="right">{money(team.totalCost)}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      {teams.length === 0 && <EmptyState />}
    </ReportPanel>
  );
}

function EpicAnalysisView({ epics, stories }: { epics: SpendByEpic[]; stories: SpendByStory[] }) {
  return (
    <Stack spacing={2.5}>
      <ReportPanel title="Cost by Epic">
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Epic</TableCell>
                <TableCell>Team</TableCell>
                <TableCell align="right">Stories</TableCell>
                <TableCell align="right">Requests</TableCell>
                <TableCell align="right">Tokens</TableCell>
                <TableCell align="right">Cost</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {epics.map((epic) => (
                <TableRow key={epic.epicKey}>
                  <TableCell>
                    <Typography fontWeight={700}>{epic.epicKey}</Typography>
                    <Typography variant="body2" color="text.secondary">{epic.epicName ?? 'No epic found'}</Typography>
                  </TableCell>
                  <TableCell>{epic.teamKey ?? 'Unknown'}</TableCell>
                  <TableCell align="right">{integer(epic.storyCount)}</TableCell>
                  <TableCell align="right">{integer(epic.requestCount)}</TableCell>
                  <TableCell align="right">{integer(epic.totalTokens)}</TableCell>
                  <TableCell align="right">{money(epic.totalCost)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      </ReportPanel>
      <ReportPanel title="Cost by Story">
        <StorySpendTable stories={stories} />
      </ReportPanel>
    </Stack>
  );
}

function StorySpendTable({ stories }: { stories: SpendByStory[] }) {
  return (
    <>
      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Story</TableCell>
              <TableCell>Epic</TableCell>
              <TableCell>Team</TableCell>
              <TableCell align="right">Requests</TableCell>
              <TableCell align="right">Tokens</TableCell>
              <TableCell align="right">Cost</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {stories.map((story) => (
              <TableRow key={story.storyKey ?? 'missing-story'}>
                <TableCell>
                  <Typography fontWeight={700}>{story.storyKey ?? 'Missing story'}</Typography>
                  <Typography variant="body2" color="text.secondary">{story.storyName ?? 'No story found'}</Typography>
                </TableCell>
                <TableCell>{story.epicKey ?? 'Unknown'}</TableCell>
                <TableCell>{story.teamKey ?? 'Unknown'}</TableCell>
                <TableCell align="right">{integer(story.requestCount)}</TableCell>
                <TableCell align="right">{integer(story.totalTokens)}</TableCell>
                <TableCell align="right">{money(story.totalCost)}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      {stories.length === 0 && <EmptyState />}
    </>
  );
}

function AttributionHealthView({ coverage, events }: { coverage: AttributionCoverage; events: UsageEvent[] }) {
  const invalidEvents = events.filter((event) => event.attributionStatus !== 'VALID');
  return (
    <Stack spacing={2.5}>
      <Box className="metric-grid">
        <MetricCard label="Coverage" value={percent(coverage.coveragePercent)} />
        <MetricCard label="Attributed cost" value={money(coverage.attributedCost)} />
        <MetricCard label="Unattributed cost" value={money(coverage.unattributedCost)} />
        <MetricCard label="Invalid events" value={integer(coverage.invalidEventCount)} />
      </Box>
      <Paper className="panel">
        <Typography variant="h2">Coverage</Typography>
        <LinearProgress variant="determinate" value={Math.min(100, Math.max(0, coverage.coveragePercent))} className="coverage-bar" />
      </Paper>
      <ReportPanel title="Invalid Events">
        <UsageEventTable events={invalidEvents} />
      </ReportPanel>
    </Stack>
  );
}

function PotentialWasteView({ waste }: { waste: PotentialWaste }) {
  const rows = [
    { label: 'Cancelled Work', value: waste.cancelledStorySpend },
    { label: 'Operational Work', value: waste.operationalSpend },
    { label: 'Unknown Attribution', value: waste.unknownAttributionSpend },
    { label: 'Failed Requests', value: waste.failedRequestSpend }
  ];
  return (
    <ReportPanel title="Potential Waste">
      <Stack spacing={1.25}>
        {rows.map((row) => (
          <Box key={row.label} className="waste-row">
            <Typography fontWeight={750}>{row.label}</Typography>
            <Typography fontWeight={750}>{money(row.value)}</Typography>
          </Box>
        ))}
      </Stack>
    </ReportPanel>
  );
}

function UsageExplorerView({
  events,
  selectedEvent,
  detailLoading,
  stories,
  onCorrected
}: {
  events: UsageEvent[];
  selectedEvent: UsageEvent | null;
  detailLoading: boolean;
  stories: SpendByStory[];
  onCorrected: (event: UsageEvent) => void;
}) {
  return (
    <Box className="requests-layout">
      <ReportPanel title="Raw Usage Events">
        <UsageEventTable events={events} />
      </ReportPanel>
      <RequestDetail event={selectedEvent} loading={detailLoading} stories={stories} onCorrected={onCorrected} />
    </Box>
  );
}

function UsageEventTable({ events }: { events: UsageEvent[] }) {
  return (
    <>
      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Request</TableCell>
              <TableCell>Model</TableCell>
              <TableCell>Attribution</TableCell>
              <TableCell align="right">Tokens</TableCell>
              <TableCell align="right">Cost</TableCell>
              <TableCell align="right">Time</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {events.map((event) => (
              <TableRow
                hover
                key={event.id}
                className="clickable-row"
                onClick={() => {
                  window.location.hash = `/request/${event.id}`;
                }}
              >
                <TableCell>
                  <Stack direction="row" alignItems="center" gap={1}>
                    <OpenInNewIcon fontSize="small" color="action" />
                    <Box className="truncate">
                      <Typography fontWeight={700}>{event.storyKey ?? 'Missing story'}</Typography>
                      <Typography variant="caption" color="text.secondary">{event.id.slice(0, 8)}</Typography>
                    </Box>
                  </Stack>
                </TableCell>
                <TableCell>{event.model}</TableCell>
                <TableCell><StatusChip value={event.attributionStatus} /></TableCell>
                <TableCell align="right">{integer(event.totalTokens)}</TableCell>
                <TableCell align="right">{money(event.estimatedCostUsd)}</TableCell>
                <TableCell align="right">{timestamp(event.requestTimestamp)}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
      {events.length === 0 && <EmptyState />}
    </>
  );
}

function MetricCard({ label, value, detail }: { label: string; value: string; detail?: string }) {
  return (
    <Paper className="metric-card">
      <Typography variant="body2" color="text.secondary">{label}</Typography>
      <Typography className="metric-value">{value}</Typography>
      {detail && <Typography variant="caption" color="text.secondary">{detail}</Typography>}
    </Paper>
  );
}

function MiniSpendList({ rows }: { rows: Array<{ key: string; label: string; cost: number; tokens: number }> }) {
  const maxCost = useMemo(() => Math.max(1, ...rows.map((row) => Number(row.cost))), [rows]);
  if (rows.length === 0) {
    return <EmptyState />;
  }
  return (
    <Stack spacing={1.25} className="mini-list">
      {rows.map((row) => (
        <Box key={row.key} className="mini-row">
          <Stack direction="row" justifyContent="space-between" gap={2}>
            <Box className="truncate">
              <Typography fontWeight={700}>{row.key}</Typography>
              <Typography variant="body2" color="text.secondary" className="truncate">{row.label}</Typography>
            </Box>
            <Typography fontWeight={700}>{money(row.cost)}</Typography>
          </Stack>
          <Box className="bar-track">
            <Box className="bar-fill" sx={{ width: `${Math.max(4, (Number(row.cost) / maxCost) * 100)}%` }} />
          </Box>
          <Typography variant="caption" color="text.secondary">{compactNumber(row.tokens)} tokens</Typography>
        </Box>
      ))}
    </Stack>
  );
}

function RequestDetail({
  event,
  loading,
  stories,
  onCorrected
}: {
  event: UsageEvent | null;
  loading: boolean;
  stories: SpendByStory[];
  onCorrected: (event: UsageEvent) => void;
}) {
  return (
    <Paper className="panel detail-panel">
      <Typography variant="h2">Request Detail</Typography>
      <Divider />
      {loading && <LinearProgress />}
      {!loading && !event && <Typography color="text.secondary">Select a request to inspect model, tokens, cost, and timing.</Typography>}
      {event && (
        <Stack spacing={1.25}>
          <DetailRow label="Model" value={event.model} />
          <DetailRow label="Provider" value={event.provider} />
          <DetailRow label="Story" value={event.storyKey ?? 'Missing'} />
          <DetailRow label="Epic" value={event.epicKey ?? 'Unknown'} />
          <DetailRow label="Team" value={event.teamKey} />
          <DetailRow label="User" value={event.userKey} />
          <DetailRow label="Prompt tokens" value={integer(event.promptTokens)} />
          <DetailRow label="Completion tokens" value={integer(event.completionTokens)} />
          <DetailRow label="Total tokens" value={integer(event.totalTokens)} />
          <DetailRow label="Cost" value={money(event.estimatedCostUsd)} />
          <DetailRow label="Timestamp" value={timestamp(event.requestTimestamp)} />
          <DetailRow label="Latency" value={`${integer(event.latencyMs)} ms`} />
          <DetailRow label="Work type" value={event.workType} />
          <DetailRow label="Request status" value={event.requestStatus} />
          <DetailRow label="Repository" value={event.repository ?? 'Unassigned'} />
          <DetailRow label="Branch" value={event.branch ?? 'Unassigned'} />
          <DetailRow label="Initiative" value={event.initiativeKey ?? 'Unassigned'} />
          <Stack spacing={0.5}>
            <Typography variant="caption" color="text.secondary">Attribution</Typography>
            <StatusChip value={event.attributionStatus} />
          </Stack>
          <CorrectionForm event={event} stories={stories} onCorrected={onCorrected} />
        </Stack>
      )}
    </Paper>
  );
}

function CorrectionForm({
  event,
  stories,
  onCorrected
}: {
  event: UsageEvent;
  stories: SpendByStory[];
  onCorrected: (event: UsageEvent) => void;
}) {
  const [storyKey, setStoryKey] = useState(event.storyKey ?? '');
  const [epicKey, setEpicKey] = useState(event.epicKey ?? '');
  const [teamKey, setTeamKey] = useState(event.teamKey ?? '');
  const [workType, setWorkType] = useState(event.workType ?? 'UNKNOWN');
  const [correctedBy, setCorrectedBy] = useState(event.userKey ?? '');
  const [note, setNote] = useState('');
  const [saving, setSaving] = useState(false);
  const [message, setMessage] = useState<string | null>(null);
  const [formError, setFormError] = useState<string | null>(null);

  useEffect(() => {
    setStoryKey(event.storyKey ?? '');
    setEpicKey(event.epicKey ?? '');
    setTeamKey(event.teamKey ?? '');
    setWorkType(event.workType ?? 'UNKNOWN');
    setCorrectedBy(event.userKey ?? '');
    setNote('');
    setMessage(null);
    setFormError(null);
  }, [event.id, event.epicKey, event.storyKey, event.teamKey, event.userKey, event.workType]);

  function selectStory(value: string) {
    setStoryKey(value);
    const story = stories.find((candidate) => candidate.storyKey === value);
    if (story) {
      setEpicKey(story.epicKey ?? '');
      setTeamKey(story.teamKey ?? '');
    }
  }

  async function submitCorrection() {
    setSaving(true);
    setMessage(null);
    setFormError(null);
    try {
      const request: AttributionCorrectionRequest = {
        storyKey: emptyToNull(storyKey),
        epicKey: emptyToNull(epicKey),
        teamKey: emptyToNull(teamKey),
        workType: emptyToNull(workType),
        correctedBy: correctedBy.trim(),
        note: emptyToNull(note)
      };
      const corrected = await api.correctAttribution(event.id, request);
      setMessage('Attribution saved');
      onCorrected(corrected);
    } catch (exception) {
      setFormError(exception instanceof Error ? exception.message : 'Unable to save attribution');
    } finally {
      setSaving(false);
    }
  }

  const canSubmit = !saving && correctedBy.trim().length > 0 && (storyKey.trim().length > 0 || teamKey.trim().length > 0);

  return (
    <Stack spacing={1.25} className="correction-form">
      <Divider />
      <Stack spacing={0.5}>
        <Typography variant="h3">Manual Attribution</Typography>
        {event.attributionCorrected && (
          <Typography variant="caption" color="text.secondary">
            Last corrected by {event.correctedBy ?? 'unknown'} {event.correctedTimestamp ? `on ${timestamp(event.correctedTimestamp)}` : ''}
          </Typography>
        )}
      </Stack>
      {message && <Alert severity="success">{message}</Alert>}
      {formError && <Alert severity="error">{formError}</Alert>}
      <Box className="form-grid">
        <TextField
          label="Story key"
          size="small"
          value={storyKey}
          onChange={(input) => selectStory(input.target.value)}
          inputProps={{ list: 'story-key-options' }}
        />
        <datalist id="story-key-options">
          {stories
            .filter((story) => story.storyKey)
            .map((story) => (
              <option key={story.storyKey ?? ''} value={story.storyKey ?? ''}>
                {story.storyName ?? story.storyKey}
              </option>
            ))}
        </datalist>
        <TextField label="Epic key" size="small" value={epicKey} onChange={(input) => setEpicKey(input.target.value)} />
        <TextField label="Team key" size="small" value={teamKey} onChange={(input) => setTeamKey(input.target.value)} />
        <TextField select label="Work type" size="small" value={workType} onChange={(input) => setWorkType(input.target.value)}>
          {['CAPITALIZED', 'OPERATIONAL', 'RESEARCH', 'SUPPORT', 'UNKNOWN'].map((value) => (
            <MenuItem key={value} value={value}>{value}</MenuItem>
          ))}
        </TextField>
        <TextField
          label="Corrected by"
          size="small"
          value={correctedBy}
          onChange={(input) => setCorrectedBy(input.target.value)}
          required
        />
        <TextField label="Note" size="small" value={note} onChange={(input) => setNote(input.target.value)} />
      </Box>
      <Button variant="contained" onClick={() => void submitCorrection()} disabled={!canSubmit}>
        {saving ? 'Saving attribution' : 'Save attribution'}
      </Button>
    </Stack>
  );
}

function StatusChip({ value }: { value: string }) {
  const color = value === 'VALID' ? 'success' : value === 'MANUAL' ? 'info' : 'warning';
  return <Chip size="small" label={value.replace(/_/g, ' ')} color={color} variant="outlined" />;
}

function DetailRow({ label, value }: { label: string; value: string }) {
  return (
    <Stack direction="row" justifyContent="space-between" gap={2}>
      <Typography variant="body2" color="text.secondary">{label}</Typography>
      <Typography variant="body2" fontWeight={700} textAlign="right">{value}</Typography>
    </Stack>
  );
}

function ReportPanel({ title, children }: { title: string; children: React.ReactNode }) {
  return (
    <Paper className="panel">
      <Typography variant="h2">{title}</Typography>
      {children}
    </Paper>
  );
}

function EmptyState() {
  return (
    <Box className="empty-state">
      <Typography color="text.secondary">No records available</Typography>
    </Box>
  );
}

function wasteTotal(waste: PotentialWaste) {
  return waste.cancelledStorySpend + waste.operationalSpend + waste.unknownAttributionSpend + waste.failedRequestSpend;
}

function emptyToNull(value: string) {
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}
