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
import FileDownloadIcon from '@mui/icons-material/FileDownload';
import GroupsIcon from '@mui/icons-material/Groups';
import HealthAndSafetyIcon from '@mui/icons-material/HealthAndSafety';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import KeyboardArrowRightIcon from '@mui/icons-material/KeyboardArrowRight';
import InsightsIcon from '@mui/icons-material/Insights';
import ListAltIcon from '@mui/icons-material/ListAlt';
import OpenInNewIcon from '@mui/icons-material/OpenInNew';
import PieChartIcon from '@mui/icons-material/PieChart';
import RefreshIcon from '@mui/icons-material/Refresh';
import SettingsIcon from '@mui/icons-material/Settings';
import UploadFileIcon from '@mui/icons-material/UploadFile';
import WarningAmberIcon from '@mui/icons-material/WarningAmber';
import { useEffect, useMemo, useState } from 'react';
import {
  AttributionCorrectionRequest,
  AttributionCoverage,
  JiraConnectionTestResult,
  ModelUtilizationSnapshot,
  OutcomeCorrelationReport,
  PilotReadinessReport,
  PotentialWaste,
  ProviderUtilizationSnapshot,
  RepositoryAnalyticsSnapshot,
  SourceControlDiagnosticsReport,
  SetupHealthComponent,
  SetupHealthReport,
  SpendAllocation,
  SpendByEpic,
  SpendByStory,
  SpendByTeam,
  SpendOverview,
  TeamAnalyticsSnapshot,
  UsageEvent,
  UsageImportResult,
  api,
  apiUrl
} from './api';
import { compactNumber, integer, money, percent, timestamp } from './format';

type View = 'overview' | 'teams' | 'epics' | 'attribution' | 'waste' | 'usage' | 'outcomes' | 'setup';

type DashboardData = {
  overview: SpendOverview;
  coverage: AttributionCoverage;
  allocation: SpendAllocation;
  waste: PotentialWaste;
  stories: SpendByStory[];
  epics: SpendByEpic[];
  teams: SpendByTeam[];
  events: UsageEvent[];
  setupHealth: SetupHealthReport;
  pilotReadiness: PilotReadinessReport;
  sourceControlDiagnostics: SourceControlDiagnosticsReport;
  teamOutcomes: TeamAnalyticsSnapshot[];
  repositoryOutcomes: RepositoryAnalyticsSnapshot[];
  outcomeCorrelations: OutcomeCorrelationReport;
  providerUtilization: ProviderUtilizationSnapshot[];
  modelUtilization: ModelUtilizationSnapshot[];
};

const viewConfig: Array<{ value: View; label: string; icon: JSX.Element }> = [
  { value: 'overview', label: 'Overview', icon: <PieChartIcon fontSize="small" /> },
  { value: 'teams', label: 'Team Analysis', icon: <GroupsIcon fontSize="small" /> },
  { value: 'epics', label: 'Epic Analysis', icon: <AccountTreeIcon fontSize="small" /> },
  { value: 'attribution', label: 'Attribution Health', icon: <HealthAndSafetyIcon fontSize="small" /> },
  { value: 'waste', label: 'Potential Waste', icon: <WarningAmberIcon fontSize="small" /> },
  { value: 'usage', label: 'Usage Explorer', icon: <ListAltIcon fontSize="small" /> },
  { value: 'outcomes', label: 'Outcomes', icon: <InsightsIcon fontSize="small" /> },
  { value: 'setup', label: 'Setup', icon: <SettingsIcon fontSize="small" /> }
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

  async function loadDashboard(showLoading = true) {
    if (showLoading) {
      setLoading(true);
    }
    setError(null);
    try {
      const [
        overview,
        coverage,
        allocation,
        waste,
        stories,
        epics,
        teams,
        events,
        setupHealth,
        pilotReadiness,
        sourceControlDiagnostics,
        teamOutcomes,
        repositoryOutcomes,
        outcomeCorrelations,
        providerUtilization,
        modelUtilization
      ] = await Promise.all([
        api.overview(),
        api.coverage(),
        api.allocation(),
        api.potentialWaste(),
        api.spendByStory(),
        api.spendByEpic(),
        api.spendByTeam(),
        api.usageEvents(),
        api.setupHealth(),
        api.pilotReadiness(),
        api.sourceControlDiagnostics(),
        api.teamEffectiveness(),
        api.repositoryAnalytics(),
        api.outcomeCorrelations(),
        api.providerUtilization(),
        api.modelUtilization()
      ]);
      setData({
        overview,
        coverage,
        allocation,
        waste,
        stories,
        epics,
        teams,
        events,
        setupHealth,
        pilotReadiness,
        sourceControlDiagnostics,
        teamOutcomes,
        repositoryOutcomes,
        outcomeCorrelations,
        providerUtilization,
        modelUtilization
      });
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Unable to load dashboard data');
    } finally {
      if (showLoading) {
        setLoading(false);
      }
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
            {route.view === 'outcomes' && (
              <OutcomeAnalyticsView teams={data.teamOutcomes} repositories={data.repositoryOutcomes} correlations={data.outcomeCorrelations} />
            )}
            {route.view === 'setup' && (
              <SetupView health={data.setupHealth} pilotReadiness={data.pilotReadiness} sourceControl={data.sourceControlDiagnostics} onImported={() => void loadDashboard(false)} />
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
      <Box className="two-column">
        <Paper className="panel">
          <Typography variant="h2">Provider Mix</Typography>
          <MiniSpendList rows={data.providerUtilization.slice(0, 5).map((provider) => ({
            key: provider.provider,
            label: `${percent(provider.costPercent)} of spend across ${integer(provider.modelCount)} models`,
            cost: provider.totalCost,
            tokens: provider.totalTokens
          }))} />
        </Paper>
        <Paper className="panel">
          <Typography variant="h2">Model Mix</Typography>
          <MiniSpendList rows={data.modelUtilization.slice(0, 5).map((model) => ({
            key: model.model,
            label: `${model.provider} / ${integer(model.requestCount)} requests / ${percent(model.costPercent)}`,
            cost: model.totalCost,
            tokens: model.totalTokens
          }))} />
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
      <CsvExportButton href="/api/v1/reports/spend/by-team.csv" label="Export teams CSV" />
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
        <CsvExportButton href="/api/v1/reports/spend/by-epic.csv" label="Export epics CSV" />
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
        <CsvExportButton href="/api/v1/reports/spend/by-story.csv" label="Export stories CSV" />
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
  const invalidEvents = events.filter((event) => !isAttributed(event));
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
        <CsvExportButton href="/api/v1/usage/events.csv?limit=100" label="Export usage CSV" />
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

function SetupView({
  health,
  pilotReadiness,
  sourceControl,
  onImported
}: {
  health: SetupHealthReport;
  pilotReadiness: PilotReadinessReport;
  sourceControl: SourceControlDiagnosticsReport;
  onImported: () => void;
}) {
  const [activeDetail, setActiveDetail] = useState<string | null>(null);

  function toggleDetail(componentKey: string) {
    setActiveDetail((current) => (current === componentKey ? null : componentKey));
  }

  const activeComponent = health.components.find((component) => component.key === activeDetail) ?? null;

  return (
    <Stack spacing={2.5}>
      <Box className="metric-grid">
        <MetricCard label="Overall setup" value={health.overallStatus.replace(/_/g, ' ')} />
        <MetricCard label="Pilot readiness" value={`${integer(pilotReadiness.score)}%`} detail={pilotReadiness.status.replace(/_/g, ' ')} />
        <MetricCard label="Ready checks" value={integer(health.components.filter((item) => item.status === 'READY').length)} detail={`${integer(health.components.length)} total checks`} />
        <MetricCard label="Warnings" value={integer(health.components.filter((item) => item.status === 'WARNING').length)} />
      </Box>
      <PilotReadinessPanel report={pilotReadiness} />
      <Box className="two-column">
        <ReportPanel title="Integration Health">
          <Stack spacing={1.25}>
            {health.components.map((component) => {
              return (
                <HealthRow
                  key={component.key}
                  component={component}
                  active={activeDetail === component.key}
                  onClick={setupDetailAvailable(component.key) ? () => toggleDetail(component.key) : undefined}
                />
              );
            })}
          </Stack>
        </ReportPanel>
        <CsvImportPanel onImported={onImported} />
      </Box>
      {activeComponent && (
        <SetupIntegrationDetail component={activeComponent} sourceControl={sourceControl} />
      )}
    </Stack>
  );
}

function PilotReadinessPanel({ report }: { report: PilotReadinessReport }) {
  return (
    <ReportPanel title="Pilot Readiness">
      <Stack spacing={1.5}>
        <Stack direction="row" justifyContent="space-between" gap={2} alignItems="center" flexWrap="wrap">
          <Typography color="text.secondary">{report.summary}</Typography>
          <StatusChip value={report.status} />
        </Stack>
        <LinearProgress variant="determinate" value={Math.min(100, Math.max(0, report.score))} className="coverage-bar" />
        <Box className="readiness-grid">
          {report.checks.map((check) => (
            <Box key={check.key} className="readiness-check">
              <Stack direction="row" justifyContent="space-between" gap={2} alignItems="center">
                <Typography fontWeight={750}>{check.label}</Typography>
                <StatusChip value={check.status} />
              </Stack>
              <Typography variant="body2" color="text.secondary">{check.message}</Typography>
            </Box>
          ))}
        </Box>
        {report.recommendedActions.length > 0 && (
          <Alert severity="info">
            <Stack spacing={0.75}>
              <Typography fontWeight={750}>Recommended next actions</Typography>
              {report.recommendedActions.map((action) => (
                <Typography key={action} variant="body2">{action}</Typography>
              ))}
            </Stack>
          </Alert>
        )}
      </Stack>
    </ReportPanel>
  );
}

function HealthRow({
  component,
  active,
  onClick
}: {
  component: SetupHealthComponent;
  active: boolean;
  onClick?: () => void;
}) {
  const rowContent = (
    <>
      <Stack direction="row" justifyContent="space-between" gap={2} alignItems="center">
        <Stack direction="row" gap={1} alignItems="center" flexWrap="wrap">
          <Typography fontWeight={750}>{component.label}</Typography>
          <StatusChip value={component.status} />
        </Stack>
        {onClick && (
          <Box className="health-row-chevron" aria-hidden="true">
            {active ? <KeyboardArrowDownIcon fontSize="small" /> : <KeyboardArrowRightIcon fontSize="small" />}
          </Box>
        )}
      </Stack>
      <Typography variant="body2" color="text.secondary">{component.message}</Typography>
    </>
  );

  if (!onClick) {
    return <Box className="health-row">{rowContent}</Box>;
  }

  return (
    <Box
      role="button"
      tabIndex={0}
      className={`health-row health-row-button${active ? ' health-row-active' : ''}`}
      onClick={onClick}
      onKeyDown={(event) => {
        if (event.key === 'Enter' || event.key === ' ') {
          event.preventDefault();
          onClick();
        }
      }}
      aria-expanded={active}
    >
      {rowContent}
    </Box>
  );
}

function setupDetailAvailable(componentKey: string) {
  return componentKey === 'sourceControl' || componentKey === 'jira' || componentKey === 'llmProxy';
}

function SetupIntegrationDetail({
  component,
  sourceControl
}: {
  component: SetupHealthComponent;
  sourceControl: SourceControlDiagnosticsReport;
}) {
  if (component.key === 'sourceControl') {
    return <SourceControlDiagnosticsPanel diagnostics={sourceControl} />;
  }
  if (component.key === 'jira') {
    return <JiraSetupPanel component={component} />;
  }
  if (component.key === 'llmProxy') {
    return <LlmProxySetupPanel component={component} />;
  }
  return null;
}

function JiraSetupPanel({ component }: { component: SetupHealthComponent }) {
  const [testing, setTesting] = useState(false);
  const [result, setResult] = useState<JiraConnectionTestResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function testConnection() {
    setTesting(true);
    setResult(null);
    setError(null);
    try {
      setResult(await api.jiraConnectionTest());
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Unable to test Jira connection');
    } finally {
      setTesting(false);
    }
  }

  return (
    <ReportPanel title="Jira Setup">
      <Stack spacing={1.5}>
        <Typography variant="body2" color="text.secondary">{component.message}</Typography>
        <Box className="metric-grid">
          <MetricCard label="Provider" value="Jira Cloud" />
          <MetricCard label="Required secret" value="JIRA API TOKEN" />
          <MetricCard label="Sync endpoint" value="/api/v1/jira/sync" />
          <MetricCard label="Fallback" value="Mock Jira" />
        </Box>
        <Box className="setup-detail-grid">
          <DetailRow label="JIRA_BASE_URL" value="https://your-site.atlassian.net" />
          <DetailRow label="JIRA_EMAIL" value="service account email" />
          <DetailRow label="JIRA_API_TOKEN" value="stored outside git" />
          <DetailRow label="JIRA_DEFAULT_JQL" value="project = KAN ORDER BY updated DESC" />
        </Box>
        <Stack direction="row" gap={1} alignItems="center" flexWrap="wrap">
          <Button variant="contained" onClick={() => void testConnection()} disabled={testing}>
            {testing ? 'Testing Jira' : 'Test Jira connection'}
          </Button>
          {result && <StatusChip value={result.status} />}
        </Stack>
        {testing && <LinearProgress />}
        {error && <Alert severity="error">Jira connection test failed: {error}</Alert>}
        {result && (
          <Box className="setup-detail-grid">
            <DetailRow label="Configured" value={result.configured ? 'Yes' : 'No'} />
            <DetailRow label="Reachable" value={result.reachable ? 'Yes' : 'No'} />
            <DetailRow label="Issues readable" value={result.issuesReadable ? 'Yes' : 'No'} />
            <DetailRow label="Issues fetched" value={integer(result.issuesFetched)} />
            <DetailRow label="Sample issue" value={result.sampleIssueKey ?? 'None'} />
            <Typography variant="body2" color="text.secondary">{result.message}</Typography>
          </Box>
        )}
        <Alert severity="info">
          Jira credentials should stay in ignored local environment files or your deployment secret store. In-app secret entry needs an encrypted credential store before we make it editable here.
        </Alert>
      </Stack>
    </ReportPanel>
  );
}

function LlmProxySetupPanel({ component }: { component: SetupHealthComponent }) {
  return (
    <ReportPanel title="LLM Proxy Setup">
      <Stack spacing={1.5}>
        <Typography variant="body2" color="text.secondary">{component.message}</Typography>
        <Box className="metric-grid">
          <MetricCard label="Proxy endpoint" value="/api/v1/proxy/openai/chat/completions" />
          <MetricCard label="Local providers" value="Mock / Ollama" />
          <MetricCard label="Paid provider" value="OpenAI-compatible" />
          <MetricCard label="Secret behavior" value="Optional locally" />
        </Box>
        <Box className="setup-detail-grid">
          <DetailRow label="LLM_PROVIDER" value="MOCK_LLM, OLLAMA, or OPENAI" />
          <DetailRow label="OPENAI_CHAT_COMPLETIONS_URL" value="provider chat completions URL" />
          <DetailRow label="OPENAI_REQUIRE_API_KEY" value="false for local mock/Ollama" />
          <DetailRow label="OPENAI_API_KEY" value="stored outside git when required" />
        </Box>
        <Alert severity="info">
          ACIP keeps usage flowing even when attribution or setup is incomplete. Provider issues should surface as visibility signals, not usage blockers.
        </Alert>
      </Stack>
    </ReportPanel>
  );
}

function SourceControlDiagnosticsPanel({ diagnostics }: { diagnostics: SourceControlDiagnosticsReport }) {
  return (
    <ReportPanel title="Source Control Diagnostics">
      <Stack spacing={1.5}>
        <Typography variant="body2" color="text.secondary">{diagnostics.message}</Typography>
        <Box className="metric-grid">
          <MetricCard label="Provider" value={diagnostics.provider.toUpperCase()} />
          <MetricCard label="Configured repos" value={integer(diagnostics.configuredRepositoryCount)} />
          <MetricCard label="Metric snapshots" value={integer(diagnostics.metricsAvailableCount)} />
          <MetricCard label="Token present" value={diagnostics.tokenPresent ? 'Yes' : 'No'} />
          <MetricCard label="Cache" value={diagnostics.cache.enabled ? (diagnostics.cache.populated ? 'Warm' : 'Cold') : 'Off'} />
          <MetricCard label="Cache TTL" value={diagnostics.cache.enabled ? `${integer(diagnostics.cache.ttlSeconds)}s` : 'Unavailable'} />
          <MetricCard label="Last loaded" value={timestamp(diagnostics.cache.lastLoadedAt)} />
          <MetricCard label="Expires" value={timestamp(diagnostics.cache.expiresAt)} />
        </Box>
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Repository</TableCell>
                <TableCell>Team</TableCell>
                <TableCell>Status</TableCell>
                <TableCell align="right">PRs</TableCell>
                <TableCell align="right">Commits</TableCell>
                <TableCell align="right">Merge Time</TableCell>
                <TableCell align="right">Review Time</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {diagnostics.repositories.map((repository) => (
                <TableRow key={`${repository.owner ?? 'mock'}-${repository.repository}`}>
                  <TableCell>
                    <Typography fontWeight={700}>{repository.repository}</Typography>
                    <Typography variant="caption" color="text.secondary">{repository.owner ?? 'No owner'}</Typography>
                  </TableCell>
                  <TableCell>{repository.teamKey || 'Unassigned'}</TableCell>
                  <TableCell><StatusChip value={repository.metricsAvailable ? 'READY' : 'WARNING'} /></TableCell>
                  <TableCell align="right">{nullableCount(repository.prCount)}</TableCell>
                  <TableCell align="right">{nullableCount(repository.commitCount)}</TableCell>
                  <TableCell align="right">{nullableHours(repository.averageMergeTimeHours)}</TableCell>
                  <TableCell align="right">{nullableHours(repository.averageReviewTimeHours)}</TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
        {diagnostics.repositories.length === 0 && <EmptyState />}
      </Stack>
    </ReportPanel>
  );
}

function CsvImportPanel({ onImported }: { onImported: () => void }) {
  const sampleCsv = `provider,model,teamKey,userKey,totalTokens,estimatedCostUsd,requestTimestamp,branch
OLLAMA,llama3.2,payments,brian,4200,0.00033600,2026-05-31T12:00:00Z,feature/PAY-1002-payment-retry`;
  const [csv, setCsv] = useState(sampleCsv);
  const [importing, setImporting] = useState(false);
  const [previewing, setPreviewing] = useState(false);
  const [resultMode, setResultMode] = useState<'preview' | 'import' | null>(null);
  const [result, setResult] = useState<UsageImportResult | null>(null);
  const [error, setError] = useState<string | null>(null);

  async function submitPreview() {
    setPreviewing(true);
    setResult(null);
    setResultMode(null);
    setError(null);
    try {
      const previewResult = await api.previewUsageCsv(csv);
      setResult(previewResult);
      setResultMode('preview');
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Unable to preview CSV');
    } finally {
      setPreviewing(false);
    }
  }

  async function submitImport() {
    setImporting(true);
    setResult(null);
    setResultMode(null);
    setError(null);
    try {
      const importResult = await api.importUsageCsv(csv);
      setResult(importResult);
      setResultMode('import');
      onImported();
    } catch (exception) {
      setError(exception instanceof Error ? exception.message : 'Unable to import CSV');
    } finally {
      setImporting(false);
    }
  }

  async function loadFile(file: File | null) {
    if (!file) {
      return;
    }
    setCsv(await file.text());
    setResult(null);
    setResultMode(null);
    setError(null);
  }

  return (
    <Paper className="panel">
      <Stack spacing={1.5}>
        <Stack direction="row" justifyContent="space-between" gap={2} alignItems="center">
          <Typography variant="h2">CSV Usage Import</Typography>
          <Stack direction="row" gap={1} alignItems="center" flexWrap="wrap" justifyContent="flex-end">
            <Button component="a" href={apiUrl('/api/v1/usage/imports/samples/minimal')} variant="outlined" size="small" startIcon={<FileDownloadIcon />}>
              Minimal
            </Button>
            <Button component="a" href={apiUrl('/api/v1/usage/imports/samples/openai')} variant="outlined" size="small" startIcon={<FileDownloadIcon />}>
              OpenAI
            </Button>
            <Button component="a" href={apiUrl('/api/v1/usage/imports/samples/advanced')} variant="outlined" size="small" startIcon={<FileDownloadIcon />}>
              Advanced
            </Button>
            <Button component="label" variant="outlined" size="small" startIcon={<UploadFileIcon />}>
              Choose CSV
              <input
                hidden
                type="file"
                accept=".csv,text/csv"
                onChange={(input) => void loadFile(input.target.files?.[0] ?? null)}
              />
            </Button>
          </Stack>
        </Stack>
        <TextField
          label="CSV payload"
          value={csv}
          onChange={(input) => {
            setCsv(input.target.value);
            setResult(null);
            setResultMode(null);
          }}
          multiline
          minRows={8}
          size="small"
        />
        <Stack direction="row" gap={1} flexWrap="wrap">
          <Button variant="outlined" onClick={() => void submitPreview()} disabled={previewing || importing || csv.trim().length === 0}>
            {previewing ? 'Previewing rows' : 'Preview import'}
          </Button>
          <Button variant="contained" onClick={() => void submitImport()} disabled={importing || previewing || csv.trim().length === 0}>
            {importing ? 'Importing usage' : 'Import usage'}
          </Button>
        </Stack>
        {error && <Alert severity="error">{error}</Alert>}
        {result && (
          <Box className="import-results">
            <Stack direction="row" justifyContent="space-between" gap={2} alignItems="center">
              <Typography variant="h3">{resultMode === 'preview' ? 'Preview Results' : 'Import Results'}</Typography>
              <StatusChip value={result.skippedCount > 0 ? 'WARNING' : 'READY'} />
            </Stack>
            <Box className="import-result-grid">
              <Paper className="import-result-card">
                <Typography variant="body2" color="text.secondary">Imported rows</Typography>
                <Typography className="metric-value">{integer(result.importedCount)}</Typography>
              </Paper>
              <Paper className="import-result-card">
                <Typography variant="body2" color="text.secondary">Skipped rows</Typography>
                <Typography className="metric-value">{integer(result.skippedCount)}</Typography>
              </Paper>
              <Paper className="import-result-card">
                <Typography variant="body2" color="text.secondary">Processed rows</Typography>
                <Typography className="metric-value">{integer(result.importedCount + result.skippedCount)}</Typography>
              </Paper>
            </Box>
            <Typography variant="body2" color="text.secondary">
              {resultMode === 'preview' ? 'Preview found' : 'Last import completed with'} {integer(result.importedCount)} valid and {integer(result.skippedCount)} skipped rows.
            </Typography>
          </Box>
        )}
        {result && resultMode === 'preview' && result.skippedCount === 0 && <Alert severity="success">CSV preview found no skipped rows.</Alert>}
        {result && resultMode === 'preview' && result.skippedCount > 0 && <Alert severity="warning">CSV preview found row-level issues.</Alert>}
        {result && resultMode === 'import' && result.skippedCount === 0 && <Alert severity="success">CSV import completed without skipped rows.</Alert>}
        {result && resultMode === 'import' && result.skippedCount > 0 && <Alert severity="warning">CSV import completed with row-level issues.</Alert>}
        {result && result.errors.length > 0 && (
          <Stack spacing={0.75}>
            {result.errors.slice(0, 5).map((item) => (
              <Typography key={`${item.rowNumber}-${item.message}`} variant="body2" color="text.secondary">
                Row {item.rowNumber}: {item.message}
              </Typography>
            ))}
          </Stack>
        )}
      </Stack>
    </Paper>
  );
}

function OutcomeAnalyticsView({
  teams,
  repositories,
  correlations
}: {
  teams: TeamAnalyticsSnapshot[];
  repositories: RepositoryAnalyticsSnapshot[];
  correlations: OutcomeCorrelationReport;
}) {
  const topTeam = teams[0];
  const topRepository = repositories[0];
  return (
    <Stack spacing={2.5}>
      <Box className="metric-grid">
        <MetricCard label="Top AI-invested team" value={topTeam?.teamKey ?? 'None'} detail={topTeam ? money(topTeam.aiSpend) : undefined} />
        <MetricCard label="Story completion" value={topTeam ? percent(topTeam.storyCompletionRate) : percent(0)} detail={topTeam?.teamKey} />
        <MetricCard label="Top repository" value={topRepository?.repository ?? 'None'} detail={topRepository ? money(topRepository.aiSpend) : undefined} />
        <MetricCard label="Repository coverage" value={topRepository ? percent(topRepository.attributionCoveragePercent) : percent(0)} />
      </Box>
      <ReportPanel title="Correlation Signals">
        <Stack spacing={1.5}>
          <Typography variant="body2" color="text.secondary">{correlations.interpretation}</Typography>
          <Box className="metric-grid">
            <MetricCard label="AI-active teams" value={`${integer(correlations.aiActiveTeamCount)} / ${integer(correlations.teamCount)}`} />
            <MetricCard label="AI-active repositories" value={`${integer(correlations.aiActiveRepositoryCount)} / ${integer(correlations.repositoryCount)}`} />
            <MetricCard label="Avg completion" value={percent(correlations.averageStoryCompletionRateForAiActiveTeams)} detail="AI-active teams" />
            <MetricCard label="Avg merge time" value={nullableHours(correlations.averageMergeTimeHoursForAiActiveRepositories)} detail="AI-active repositories" />
          </Box>
          <TableContainer>
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Subject</TableCell>
                  <TableCell align="right">AI Spend</TableCell>
                  <TableCell>Metric</TableCell>
                  <TableCell align="right">Value</TableCell>
                  <TableCell>Signal</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {correlations.signals.map((signal) => (
                  <TableRow key={`${signal.subjectType}-${signal.subjectKey}-${signal.outcomeMetric}`}>
                    <TableCell>
                      <Typography fontWeight={700}>{signal.subjectKey}</Typography>
                      <Typography variant="caption" color="text.secondary">{signal.subjectType}</Typography>
                    </TableCell>
                    <TableCell align="right">{money(signal.aiSpend)}</TableCell>
                    <TableCell>{signal.outcomeMetric.replace(/([A-Z])/g, ' $1')}</TableCell>
                    <TableCell align="right">{formatSignalValue(signal.outcomeMetric, signal.outcomeValue)}</TableCell>
                    <TableCell>
                      <Typography variant="body2">{signal.signal}</Typography>
                      <Typography variant="caption" color="text.secondary">{signal.interpretation}</Typography>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </TableContainer>
          {correlations.signals.length === 0 && <EmptyState />}
        </Stack>
      </ReportPanel>
      <ReportPanel title="Team Effectiveness">
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Team</TableCell>
                <TableCell align="right">AI Spend</TableCell>
                <TableCell align="right">Requests</TableCell>
                <TableCell align="right">Stories</TableCell>
                <TableCell align="right">Completed</TableCell>
                <TableCell align="right">Cancelled</TableCell>
                <TableCell align="right">Operations</TableCell>
                <TableCell>Status</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {teams.map((team) => (
                <TableRow key={team.teamKey}>
                  <TableCell>
                    <Typography fontWeight={700}>{team.teamKey}</Typography>
                    <Typography variant="caption" color="text.secondary">{team.interpretation}</Typography>
                  </TableCell>
                  <TableCell align="right">{money(team.aiSpend)}</TableCell>
                  <TableCell align="right">{integer(team.aiRequestCount)}</TableCell>
                  <TableCell align="right">{integer(team.storyCount)}</TableCell>
                  <TableCell align="right">{percent(team.storyCompletionRate)}</TableCell>
                  <TableCell align="right">{percent(team.cancelledStoryRate)}</TableCell>
                  <TableCell align="right">{percent(team.operationalWorkRate)}</TableCell>
                  <TableCell><StatusChip value={team.outcomeDataStatus} /></TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
        {teams.length === 0 && <EmptyState />}
      </ReportPanel>
      <ReportPanel title="Repository Analytics">
        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Repository</TableCell>
                <TableCell align="right">AI Spend</TableCell>
                <TableCell align="right">Requests</TableCell>
                <TableCell align="right">Tokens</TableCell>
                <TableCell align="right">Coverage</TableCell>
                <TableCell align="right">PRs</TableCell>
                <TableCell align="right">Commits</TableCell>
                <TableCell align="right">Merge Time</TableCell>
                <TableCell align="right">Review Time</TableCell>
                <TableCell>Status</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {repositories.map((repository) => (
                <TableRow key={repository.repository}>
                  <TableCell>
                    <Typography fontWeight={700}>{repository.repository}</Typography>
                    <Typography variant="caption" color="text.secondary">
                      {[repository.owner, repository.teamKey].filter(Boolean).join(' / ') || 'No source-control owner'}
                    </Typography>
                    <br />
                    <Typography variant="caption" color="text.secondary">{repository.interpretation}</Typography>
                  </TableCell>
                  <TableCell align="right">{money(repository.aiSpend)}</TableCell>
                  <TableCell align="right">{integer(repository.aiRequestCount)}</TableCell>
                  <TableCell align="right">{integer(repository.totalTokens)}</TableCell>
                  <TableCell align="right">{percent(repository.attributionCoveragePercent)}</TableCell>
                  <TableCell align="right">{nullableCount(repository.prCount)}</TableCell>
                  <TableCell align="right">{nullableCount(repository.commitCount)}</TableCell>
                  <TableCell align="right">{nullableHours(repository.averageMergeTimeHours)}</TableCell>
                  <TableCell align="right">{nullableHours(repository.averageReviewTimeHours)}</TableCell>
                  <TableCell><StatusChip value={repository.outcomeDataStatus} /></TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
        {repositories.length === 0 && <EmptyState />}
      </ReportPanel>
    </Stack>
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
          <DetailRow label="Capture source" value={event.captureSource.replace(/_/g, ' ')} />
          <DetailRow label="Capture provider" value={event.captureProvider.replace(/_/g, ' ')} />
          <DetailRow label="Capture method" value={event.captureMethod.replace(/_/g, ' ')} />
          <DetailRow label="Capture confidence" value={event.captureConfidence} />
          <DetailRow label="Attribution source" value={event.attributionSource.replace(/_/g, ' ')} />
          <DetailRow label="Confidence" value={event.attributionConfidence} />
          <DetailRow label="Inferred story" value={event.inferredStoryKey ?? 'None'} />
          <DetailRow label="Inference reason" value={event.inferenceReason ?? 'None'} />
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
  const color = value === 'VALID' || value === 'READY' || value === 'AVAILABLE' ? 'success' : value === 'MANUAL' || value === 'PARTIAL' ? 'info' : value === 'ERROR' ? 'error' : 'warning';
  return <Chip size="small" label={value.replace(/_/g, ' ')} color={color} variant="outlined" />;
}

function isAttributed(event: UsageEvent) {
  return event.attributionStatus === 'VALID' || event.attributionStatus === 'MANUAL';
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

function CsvExportButton({ href, label }: { href: string; label: string }) {
  return (
    <Box className="panel-action-row">
      <Button
        component="a"
        href={apiUrl(href)}
        size="small"
        variant="outlined"
        startIcon={<FileDownloadIcon fontSize="small" />}
      >
        {label}
      </Button>
    </Box>
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

function nullableCount(value: number | null) {
  return value == null ? 'Unavailable' : integer(value);
}

function nullableHours(value: number | null) {
  return value == null ? 'Unavailable' : `${Number(value).toFixed(1)}h`;
}

function formatSignalValue(metric: string, value: number) {
  if (metric.toLowerCase().includes('rate')) {
    return percent(value);
  }
  if (metric.toLowerCase().includes('time')) {
    return `${Number(value).toFixed(1)}h`;
  }
  return integer(value);
}

function emptyToNull(value: string) {
  const trimmed = value.trim();
  return trimmed.length > 0 ? trimmed : null;
}
