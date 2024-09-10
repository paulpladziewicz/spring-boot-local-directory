package com.paulpladziewicz.fremontmi.controllers;

import com.paulpladziewicz.fremontmi.models.Business;
import com.paulpladziewicz.fremontmi.models.NeighborService;
import com.paulpladziewicz.fremontmi.models.PaymentRequest;
import com.paulpladziewicz.fremontmi.models.ServiceResponse;
import com.paulpladziewicz.fremontmi.services.NeighborServiceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@Controller
public class NeighborServiceController {

    private final NeighborServiceService neighborServiceService;

    public NeighborServiceController(NeighborServiceService neighborServiceService) {
        this.neighborServiceService = neighborServiceService;
    }

    @GetMapping("/create/neighbor-service/overview")
    public String createNeighborServiceOverview(Model model) {
        return "neighborservices/neighborservices-create-overview";
    }

    @PostMapping("/create/neighbor-service/form")
    public String createNeighborServiceView(@RequestParam("priceId") String priceId, @RequestParam(value = "neighborServiceId", required = false) String existingNeighborServiceId, Model model) {
        if (priceId == null || priceId.isEmpty() || priceId.trim().isEmpty()) {
            model.addAttribute("error", "Pricing appears to be tampered with. Please refresh the page and try again.");
            return "neighborservices/neighborservices-create-overview";
        }

        if (!priceId.equals("price_1Pv7TzBCHBXtJFxOWmV5PE4h") && !priceId.equals("price_1PELXZBCHBXtJFxO4FchTfAv")) {
            model.addAttribute("error", "Pricing appears to be tampered with. Please refresh the page and try again.");
            return "neighborservices/neighborservices-create-overview";
        }

        if (existingNeighborServiceId != null && !existingNeighborServiceId.isEmpty() && !existingNeighborServiceId.trim().isEmpty()) {
            Optional<NeighborService> optionalNeighborService = neighborServiceService.findNeighborServiceById(existingNeighborServiceId);

            if (optionalNeighborService.isEmpty()) {
                model.addAttribute("errorMessage", "Neighbor service not found. Please refresh the page and try again.");
                return "neighborservices/neighborservices-create-overview";
            }

            NeighborService existingNeighborService = optionalNeighborService.get();

            existingNeighborService.setSubscriptionPriceId(priceId);

            model.addAttribute("neighborService", existingNeighborService);

            return "neighborservices/neighborservices-create-form";
        }

        ServiceResponse<List<NeighborService>> findNeighborServicesByUserResponse = neighborServiceService.findNeighborServicesByUser();

        if (findNeighborServicesByUserResponse.hasError()) {
            model.addAttribute("errorMessage", "An error occurred while trying to retrieve your neighbor services. Please try again later.");
            return "neighborservices/neighborservices-create-overview";
        }

        List<NeighborService> neighborServices = findNeighborServicesByUserResponse.value();
        List<NeighborService> incompleteNeighborServices = new ArrayList<>();

        for (NeighborService neighborService : neighborServices) {
            if (neighborService.getStatus().equals("incomplete")) {
                neighborService.setSubscriptionPriceId(priceId);
                incompleteNeighborServices.add(neighborService);
            }
        }

        if (!incompleteNeighborServices.isEmpty()) {
            model.addAttribute("incompleteNeighborServices", incompleteNeighborServices);
            return "neighborservices/neighborservices-continue-progress";
        }

        NeighborService neighborService = new NeighborService();
        neighborService.setSubscriptionPriceId(priceId);

        model.addAttribute("neighborService", neighborService);

        return "neighborservices/neighborservices-create-form";
    }

    @PostMapping("/create/neighbor-service/subscription")
    public String createNeighborServiceSubscription(@Valid @ModelAttribute("neighborService") NeighborService neighborService, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("neighborService", neighborService);
            return "neighborservices/neighborservices-create-form";
        }

        String priceId = neighborService.getSubscriptionPriceId();

        if (priceId == null || priceId.isEmpty() || priceId.trim().isEmpty()) {
            model.addAttribute("error", "Pricing appears to be tampered with. Please refresh the page and try again.");
            return "businesses/business-create-overview";
        }

        if (!priceId.equals("price_1Pv7TzBCHBXtJFxOWmV5PE4h") && !priceId.equals("price_1PELXZBCHBXtJFxO4FchTfAv")) {
            model.addAttribute("errorMessage", "Pricing appears to be tampered with. Please refresh the page and try again.");
            return "neighborservices/neighborservices-create-overview";
        }

        ServiceResponse<NeighborService> createNeighborServiceResponse = neighborServiceService.createNeighborService(neighborService);

        if (createNeighborServiceResponse.hasError()) {
            model.addAttribute("errorMessage", "An error occurred while trying to create the neighbor service. Please try again later.");
            return "neighborservices/neighborservices-create-overview";
        }

        NeighborService createdNeighborService = createNeighborServiceResponse.value();

        model.addAttribute("neighborService", createdNeighborService);
        model.addAttribute("planName", "Monthly NeighborService™ Subscription");
        model.addAttribute("price", "$5/month");

        return "neighborservices/neighborservices-create-subscription";
    }

    @PostMapping("/create/neighbor-service/subscription/success")
    @ResponseBody
    public ResponseEntity<?> handleSubscriptionSuccess(@RequestBody PaymentRequest paymentRequest) {
        ServiceResponse<NeighborService> serviceResponse = neighborServiceService.handleSubscriptionSuccess(paymentRequest);

        if (serviceResponse.hasError()) {
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("error", serviceResponse.errorCode()));
        }

        NeighborService neighborService = serviceResponse.value();

        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("redirectUrl", "/neighbor-services/" + neighborService.getId());

        return ResponseEntity.ok(responseBody);
    }

    @GetMapping("/neighbor-services")
    public String displayActiveNeighborServices(Model model) {
        ServiceResponse<List<NeighborService>> findAllResponse = neighborServiceService.findAllActiveNeighborServices();

        if (findAllResponse.hasError()) {
            model.addAttribute("errorMessage", "An error occurred while trying to retrieve the neighbor services. Please try again later.");
            return "neighborservices/neighborservices";
        }

        List<NeighborService> neighborServices = findAllResponse.value();

        model.addAttribute("neighborServices", neighborServices);

        return "neighborservices/neighborservices";
    }

    @GetMapping("/neighbor-services/{id}")
    public String viewNeighborService(@PathVariable String id, Model model) {
        Optional<NeighborService> optionalNeighborService = neighborServiceService.findNeighborServiceById(id);

        if (optionalNeighborService.isEmpty()) {
            model.addAttribute("errorMessage", "Neighbor service not found. Please try again later.");
            return "neighborservices/neighborservices-page";
        }

        NeighborService neighborService = optionalNeighborService.get();

        model.addAttribute("neighborService", neighborService);

        return "neighborservices/neighborservices-page";
    }

    @GetMapping("/my/neighbor-services")
    public String displayMyNeighborServices(Model model) {
        ServiceResponse<List<NeighborService>> findNeighborServicesByUserResponse = neighborServiceService.findNeighborServicesByUser();

        if (findNeighborServicesByUserResponse.hasError()) {
            model.addAttribute("errorMessage", "An error occurred while trying to retrieve your neighbor services. Please try again later.");
            return "neighborservices/my-neighborservices";
        }

        List<NeighborService> neighborServices = findNeighborServicesByUserResponse.value();

        model.addAttribute("neighborServices", neighborServices);

        return "neighborservices/my-neighborservices";
    }

    @GetMapping("/edit/neighbor-service/{id}")
    public String editBusiness(@PathVariable String id, Model model) {
        Optional<NeighborService> optionalNeighborService = neighborServiceService.findNeighborServiceById(id);

        if (optionalNeighborService.isEmpty()) {
            model.addAttribute("error", true);
            return "businesses/businesses";
        }

        NeighborService neighborService = optionalNeighborService.get();

        model.addAttribute("neighborService", neighborService);

        return "neighborservices/edit-neighborservice";
    }


    @PostMapping("/edit/neighbor-service")
    public String editNeighborService(@Valid @ModelAttribute("neighborService") NeighborService neighborService, BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("neighborService", neighborService);
            return "neighborservices/edit-neighborservice";
        }

        ServiceResponse<NeighborService> updateNeighborServiceResponse = neighborServiceService.updateNeighborService(neighborService);

        if (updateNeighborServiceResponse.hasError()) {
            model.addAttribute("errorMessage", "An error occurred while trying to update the neighbor service. Please try again later.");
            return "neighborservices/edit-neighborservice";
        }

        NeighborService updatedNeighborService = updateNeighborServiceResponse.value();

        model.addAttribute("neighborService", updatedNeighborService);

        return "redirect:/neighbor-services/" + updatedNeighborService.getId();
    }
}
