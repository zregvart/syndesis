import * as H from '@syndesis/history';
import * as React from 'react';

import {
  ApiConnectorCreatorBreadcrumb,
  ApiConnectorCreatorBreadSteps,
  ApiConnectorCreatorDetails,
  ApiConnectorCreatorFooter,
  ApiConnectorCreatorLayout,
  ApiConnectorCreatorToggleList,
} from '@syndesis/ui';
import { ApiConnectorInfoForm, IConnectorValues } from '../../components';

import { useApiConnectorCreator } from '@syndesis/api';
import { IApiSummarySoap } from '@syndesis/models';
import { useRouteData } from '@syndesis/utils';
import { useTranslation } from 'react-i18next';
import { UIContext } from '../../../../app';
import { PageTitle } from '../../../../shared';
import { WithLeaveConfirmation } from '../../../../shared/WithLeaveConfirmation';
import { ICreateConnectorProps } from '../../models';
import resolvers from '../../resolvers';
import routes from '../../routes';

export interface IDetailsPageRouteState {
  configured?: ICreateConnectorProps;
  connectorTemplateId?: string;
  specification: IApiSummarySoap;
}

export const DetailsPage: React.FunctionComponent = () => {
  const { t } = useTranslation(['apiClientConnectors', 'shared']);
  const { pushNotification } = React.useContext(UIContext);
  const { state, history } = useRouteData<null, IDetailsPageRouteState>();
  const createApiConnector = useApiConnectorCreator();

  return (
    <WithLeaveConfirmation
      i18nTitle={t('apiClientConnectors:create:unsavedChangesTitle')}
      i18nConfirmationMessage={t(
        'apiClientConnectors:create:unsavedChangesMessage'
      )}
      shouldDisplayDialog={(location: H.LocationDescriptor) => {
        const url =
          typeof location === 'string' ? location : location.pathname!;
        return !url.startsWith(routes.create.root);
      }}
    >
      {({ allowNavigation }) => {
        const onSubmit = async (values: IConnectorValues, actions: any) => {
          actions.setSubmitting(true);

          try {
            await createApiConnector({
              ...state.configured,
              ...values,
              connectorTemplateId: state.connectorTemplateId,
              specification:
                state.specification.configuredProperties!.specification,
            });
            actions.setSubmitting(false);
            allowNavigation();
            history.push(resolvers.list());
            pushNotification(
              t('apiClientConnectors:create:details:successNotification'),
              'success'
            );
            return true;
          } catch (e) {
            actions.setSubmitting(false);
            pushNotification(e.message, 'error');
            return false;
          }
        };

        return (
          <>
            <PageTitle title={t('apiClientConnectors:create:details:title')} />
            <ApiConnectorCreatorBreadcrumb
              cancelHref={resolvers.list()}
              connectorsHref={resolvers.list()}
              i18nCancel={t('shared:Cancel')}
              i18nConnectors={t('apiClientConnectors:apiConnectorsPageTitle')}
              i18nCreateConnection={t('apiClientConnectors:CreateApiConnector')}
            />
            <ApiConnectorInfoForm
              name={state.specification.name}
              description={state.specification.description}
              connectorTemplateId={state.connectorTemplateId}
              basePath={
                state.specification.configuredProperties?.basePath ||
                state.specification.properties?.basePath?.defaultValue
              }
              host={
                state.specification.configuredProperties?.host ||
                state.specification.properties?.host?.defaultValue
              }
              address={
                state.specification.configuredProperties?.address ||
                state.specification.properties?.address?.defaultValue
              }
              handleSubmit={onSubmit}
            >
              {({
                connectorName,
                fields,
                handleSubmit,
                icon,
                isSubmitting,
                isUploadingImage,
                onUploadImage,
                submitForm,
              }) => (
                <ApiConnectorCreatorLayout
                  content={
                    <div style={{ maxWidth: '600px' }}>
                      <ApiConnectorCreatorDetails
                        apiConnectorIcon={icon}
                        apiConnectorName={connectorName}
                        i18nIconLabel={t('ConnectorIcon')}
                        handleSubmit={handleSubmit}
                        onUploadImage={onUploadImage}
                        isEditing={true}
                        fields={fields}
                      />
                    </div>
                  }
                  footer={
                    <ApiConnectorCreatorFooter
                      backHref={resolvers.create.security(state)}
                      onNext={submitForm}
                      i18nBack={t('shared:Back')}
                      i18nNext={t('shared:Save')}
                      isNextLoading={isSubmitting || isUploadingImage}
                      isNextDisabled={isSubmitting || isUploadingImage}
                    />
                  }
                  navigation={
                    <ApiConnectorCreatorBreadSteps
                      step={4}
                      i18nConfiguration={t(
                        'apiClientConnectors:create:configuration:title'
                      )}
                      i18nDetails={t(
                        'apiClientConnectors:create:details:title'
                      )}
                      i18nReview={t('apiClientConnectors:create:review:title')}
                      i18nSecurity={t(
                        'apiClientConnectors:create:security:title'
                      )}
                      i18nSelectMethod={t(
                        'apiClientConnectors:create:selectMethod:title'
                      )}
                    />
                  }
                  toggle={
                    <ApiConnectorCreatorToggleList
                      step={1}
                      i18nDetails={t(
                        'apiClientConnectors:create:details:title'
                      )}
                      i18nReview={t('apiClientConnectors:create:review:title')}
                      i18nSecurity={t(
                        'apiClientConnectors:create:security:title'
                      )}
                      i18nSelectMethod={t(
                        'apiClientConnectors:create:selectMethod:title'
                      )}
                    />
                  }
                />
              )}
            </ApiConnectorInfoForm>
          </>
        );
      }}
    </WithLeaveConfirmation>
  );
};
