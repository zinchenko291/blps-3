package me.zinch.itmo.mts.web.manager;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import me.zinch.itmo.mts.domain.entity.User;
import me.zinch.itmo.mts.service.manager.ManagerService;
import me.zinch.itmo.mts.web.manager.dto.ManagerDto;
import me.zinch.itmo.mts.web.manager.dto.ManagerListItemResponse;

@RestController
@RequestMapping("/api/managers")
public class ManagerController {

    private final ManagerService managerService;

    public ManagerController(ManagerService managerService) {
        this.managerService = managerService;
    }

    @GetMapping
    public Page<ManagerListItemResponse> getManagers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "name"));
        return managerService.getManagersForSenior(pageable).map(this::toResponse);
    }

    private ManagerListItemResponse toResponse(User user) {
        return new ManagerListItemResponse(user.getId(), new ManagerDto(user.getName()));
    }
}
