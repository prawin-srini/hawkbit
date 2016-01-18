package org.eclipse.hawkbit.repository;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;

import org.eclipse.hawkbit.ControllerPollProperties;
import org.eclipse.hawkbit.repository.model.TenantMetaData;
import org.eclipse.hawkbit.repository.model.helper.PollConfigurationHelper;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PollConfigurationHelperTest {

    @Mock
    private ControllerPollProperties controllerPollProperties;

    @Mock
    private SystemManagement systemManagement;

    @Mock
    private TenantMetaData tenantMetaData;

    private PollConfigurationHelper pollConfigurationHelperUnderTest;

    private static final Duration DEFAULT_MIN = Duration.ofSeconds(30);
    private static final Duration DEFAULT_MAX = Duration.ofHours(23).plusMinutes(59).plusSeconds(59);
    private static final Duration DEFAULT_POLLING = Duration.ofMinutes(5);
    private static final Duration DEFAULT_OVERDUE = Duration.ofMinutes(5);

    @Before
    public void initMocks() {

        pollConfigurationHelperUnderTest = PollConfigurationHelper.getInstance();

        setConfigurationValues("00:05:00", "00:05:00", "00:00:30", "23:59:59");
        setTenantConfiguration(null, null);
    }

    private void setConfigurationValues(String polling, String overdue, String min, String max) {
        when(controllerPollProperties.getPollingTime()).thenReturn(polling);
        when(controllerPollProperties.getPollingOverdueTime()).thenReturn(overdue);
        when(controllerPollProperties.getMinPollingTime()).thenReturn(min);
        when(controllerPollProperties.getMaxPollingTime()).thenReturn(max);

        pollConfigurationHelperUnderTest.setControllerPollProperties(controllerPollProperties);
        pollConfigurationHelperUnderTest.initializeConfigurationValues();
    }

    private void setTenantConfiguration(String polling, String overdue) {

        when(tenantMetaData.getPollingTime()).thenReturn(polling);
        when(tenantMetaData.getPollingOverdueTime()).thenReturn(overdue);

        when(systemManagement.getTenantMetadata()).thenReturn(tenantMetaData);

        pollConfigurationHelperUnderTest.setSystemManagement(systemManagement);
    }

    @Test
    public void getCorrectConfigurationValues() {

        setConfigurationValues("00:08:00", "00:12:00", "00:01:00", "20:00:00");

        assertThat(pollConfigurationHelperUnderTest.getMaximumPollingInterval()).isEqualTo(Duration.ofHours(20));
        assertThat(pollConfigurationHelperUnderTest.getMinimumPollingInterval()).isEqualTo(Duration.ofMinutes(1));
        assertThat(pollConfigurationHelperUnderTest.getGlobalPollTimeInterval()).isEqualTo(Duration.ofMinutes(8));
        assertThat(pollConfigurationHelperUnderTest.getGlobalOverduePollTimeInterval())
                .isEqualTo(Duration.ofMinutes(12));

        assertThat(pollConfigurationHelperUnderTest.getPollTimeInterval()).isEqualTo(Duration.ofMinutes(8));
        assertThat(pollConfigurationHelperUnderTest.getOverduePollTimeInterval()).isEqualTo(Duration.ofMinutes(12));
    }

    @Test
    public void getWrongFromattedConfiguratonValues() {
        setConfigurationValues("00-08:00", "abc", "12:00:000", "20hours");

        assertThat(pollConfigurationHelperUnderTest.getMaximumPollingInterval()).isEqualTo(DEFAULT_MAX);
        assertThat(pollConfigurationHelperUnderTest.getMinimumPollingInterval()).isEqualTo(DEFAULT_MIN);
        assertThat(pollConfigurationHelperUnderTest.getGlobalPollTimeInterval()).isEqualTo(DEFAULT_POLLING);
        assertThat(pollConfigurationHelperUnderTest.getGlobalOverduePollTimeInterval()).isEqualTo(DEFAULT_OVERDUE);
    }

    @Test
    public void getMinimumGreaterMaximum() {
        setConfigurationValues("00:07:00", "00:07:00", "01:00:00", "00:00:00");

        assertThat(pollConfigurationHelperUnderTest.getMaximumPollingInterval()).isEqualTo(DEFAULT_MAX);
        assertThat(pollConfigurationHelperUnderTest.getMinimumPollingInterval()).isEqualTo(DEFAULT_MIN);
        assertThat(pollConfigurationHelperUnderTest.getGlobalPollTimeInterval()).isEqualTo(Duration.ofMinutes(7));
        assertThat(pollConfigurationHelperUnderTest.getGlobalOverduePollTimeInterval())
                .isEqualTo(Duration.ofMinutes(7));
    }

    @Test
    public void getPollConfigurationNotWithinRange() {
        setConfigurationValues("22:00:00", "00:07:00", "01:00:00", "10:00:00");

        assertThat(pollConfigurationHelperUnderTest.getMaximumPollingInterval()).isEqualTo(Duration.ofHours(10));
        assertThat(pollConfigurationHelperUnderTest.getMinimumPollingInterval()).isEqualTo(Duration.ofHours(1));
        assertThat(pollConfigurationHelperUnderTest.getGlobalPollTimeInterval()).isEqualTo(DEFAULT_POLLING);
        assertThat(pollConfigurationHelperUnderTest.getGlobalOverduePollTimeInterval()).isEqualTo(DEFAULT_OVERDUE);
    }

    @Test
    public void getPollingValuesFromTenant() {

        setTenantConfiguration("00:11:00", "00:13:00");

        assertThat(pollConfigurationHelperUnderTest.getPollTimeInterval()).isEqualTo(Duration.ofMinutes(11));
        assertThat(pollConfigurationHelperUnderTest.getOverduePollTimeInterval()).isEqualTo(Duration.ofMinutes(13));

    }

    @Test
    public void getInvalidPollingValuesFromTenant() {

        setTenantConfiguration("00:00:01", "00:130:00");

        assertThat(pollConfigurationHelperUnderTest.getPollTimeInterval()).isEqualTo(DEFAULT_POLLING);
        assertThat(pollConfigurationHelperUnderTest.getOverduePollTimeInterval()).isEqualTo(DEFAULT_OVERDUE);

    }

    @Test
    public void setTenantConfiguration() {
        pollConfigurationHelperUnderTest.setTenantPollTimeIntervall(Duration.ofHours(3).plusSeconds(3));
        pollConfigurationHelperUnderTest.setTenantOverduePollTimeIntervall(Duration.ofMinutes(7).plusSeconds(7));

        verify(tenantMetaData, times(1)).setPollingTime("03:00:03");
        verify(tenantMetaData, times(1)).setPollingOverdueTime("00:07:07");
    }

    @Test
    public void setTenantConfigurationToNull() {
        pollConfigurationHelperUnderTest.setTenantPollTimeIntervall(null);
        pollConfigurationHelperUnderTest.setTenantOverduePollTimeIntervall(null);

        verify(tenantMetaData, times(1)).setPollingTime(null);
        verify(tenantMetaData, times(1)).setPollingOverdueTime(null);
    }

}
